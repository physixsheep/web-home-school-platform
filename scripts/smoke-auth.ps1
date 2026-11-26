param(
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$tempBase = [IO.Path]::GetTempPath().TrimEnd("\\")
$runRoot = Join-Path $tempBase "homeschool-auth-smoke-$PID"
$dataDir = Join-Path $runRoot "mysql-data"
$mysqlBase = "D:\\soft\\mysql"
$mysqld = Join-Path $mysqlBase "bin\\mysqld.exe"
$mysql = Join-Path $mysqlBase "bin\\mysql.exe"
$mysqladmin = Join-Path $mysqlBase "bin\\mysqladmin.exe"
$mysqlPort = 33306
$apiPort = 18080
$mysqlProcess = $null
$apiProcess = $null

function Wait-ForHttp {
    param(
        [string]$Uri,
        [int]$TimeoutSeconds = 45
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw "Timed out waiting for $Uri"
}

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [hashtable]$Body,
        [string]$Token = ""
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }
    $json = $Body | ConvertTo-Json -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    return Invoke-RestMethod `
        -Method Post `
        -Uri $Uri `
        -Headers $headers `
        -ContentType "application/json; charset=utf-8" `
        -Body $bytes
}

function Invoke-JsonPatch {
    param(
        [string]$Uri,
        [hashtable]$Body,
        [string]$Token
    )

    $json = $Body | ConvertTo-Json -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    return Invoke-RestMethod `
        -Method Patch `
        -Uri $Uri `
        -Headers @{ Authorization = "Bearer $Token" } `
        -ContentType "application/json; charset=utf-8" `
        -Body $bytes
}

function Invoke-JsonPut {
    param(
        [string]$Uri,
        [hashtable]$Body,
        [string]$Token
    )

    $json = $Body | ConvertTo-Json -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    return Invoke-RestMethod `
        -Method Put `
        -Uri $Uri `
        -Headers @{ Authorization = "Bearer $Token" } `
        -ContentType "application/json; charset=utf-8" `
        -Body $bytes
}

function Invoke-Delete {
    param(
        [string]$Uri,
        [string]$Token
    )

    return Invoke-RestMethod `
        -Method Delete `
        -Uri $Uri `
        -Headers @{ Authorization = "Bearer $Token" }
}

function Get-HttpStatus {
    param(
        [scriptblock]$Action
    )

    try {
        & $Action | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

function Invoke-NativeCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $FilePath @Arguments 2>&1 | Out-Null
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

if (-not $JavaHome) {
    $candidate = Join-Path $env:USERPROFILE ".jdks\\ms-17.0.17"
    if (Test-Path -LiteralPath $candidate) {
        $JavaHome = $candidate
    }
}

if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\\java.exe"))) {
    throw "JDK 17 was not found. Pass its directory with the -JavaHome parameter."
}

if (-not (Test-Path -LiteralPath $mysqld)) {
    throw "MySQL server binary was not found at $mysqld."
}

New-Item -ItemType Directory -Path $runRoot -Force | Out-Null
New-Item -ItemType Directory -Path $dataDir -Force | Out-Null

try {
    $initializeExitCode = Invoke-NativeCommand `
        -FilePath $mysqld `
        -Arguments @(
            "--no-defaults",
            "--initialize-insecure",
            "--basedir=$mysqlBase",
            "--datadir=$dataDir"
        )
    if ($initializeExitCode -ne 0) {
        throw "Failed to initialize the temporary MySQL data directory."
    }

    $mysqlProcess = Start-Process `
        -FilePath $mysqld `
        -ArgumentList @(
            "--no-defaults",
            "--console",
            "--basedir=$mysqlBase",
            "--datadir=$dataDir",
            "--port=$mysqlPort",
            "--bind-address=127.0.0.1",
            "--mysqlx=0",
            "--pid-file=$dataDir\\mysqld.pid",
            "--log-error=$runRoot\\mysql-error.log"
        ) `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runRoot "mysql.stdout.log") `
        -RedirectStandardError (Join-Path $runRoot "mysql.stderr.log") `
        -PassThru

    $mysqlDeadline = (Get-Date).AddSeconds(45)
    $mysqlReady = $false
    while ((Get-Date) -lt $mysqlDeadline -and -not $mysqlReady) {
        $pingExitCode = Invoke-NativeCommand `
            -FilePath $mysqladmin `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "ping"
            )
        $mysqlReady = $pingExitCode -eq 0
        if (-not $mysqlReady) {
            Start-Sleep -Milliseconds 500
        }
    }
    if (-not $mysqlReady) {
        $serverLog = Join-Path $runRoot "mysql-error.log"
        $standardError = Join-Path $runRoot "mysql.stderr.log"
        $details = @()
        if (Test-Path -LiteralPath $serverLog) {
            $details += Get-Content -LiteralPath $serverLog -Tail 80
        }
        if (Test-Path -LiteralPath $standardError) {
            $details += Get-Content -LiteralPath $standardError -Tail 80
        }
        throw "Temporary MySQL instance did not become ready.`n$($details -join "`n")"
    }

    Push-Location $projectRoot
    try {
        $createDatabaseExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "--execute=source database/00_create_database.sql"
            )
        if ($createDatabaseExitCode -ne 0) {
            throw "Failed to create the smoke-test database."
        }

        $schemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/01_foundation_schema.sql"
            )
        if ($schemaExitCode -ne 0) {
            throw "Failed to create the foundation tables."
        }

        $seedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/02_foundation_data.sql"
            )
        if ($seedExitCode -ne 0) {
            throw "Failed to seed the foundation data."
        }

        $noticeSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/03_notice_schema.sql"
            )
        if ($noticeSchemaExitCode -ne 0) {
            throw "Failed to create notice tables."
        }

        $noticeSeedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/04_notice_data.sql"
            )
        if ($noticeSeedExitCode -ne 0) {
            throw "Failed to seed notice data."
        }

        $assignmentSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/05_assignment_schema.sql"
            )
        if ($assignmentSchemaExitCode -ne 0) {
            throw "Failed to create assignment tables."
        }

        $assignmentSeedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/06_assignment_data.sql"
            )
        if ($assignmentSeedExitCode -ne 0) {
            throw "Failed to seed assignment data."
        }

        $scoreSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/07_score_schema.sql"
            )
        if ($scoreSchemaExitCode -ne 0) {
            throw "Failed to create score tables."
        }

        $scoreSeedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/08_score_data.sql"
            )
        if ($scoreSeedExitCode -ne 0) {
            throw "Failed to seed score data."
        }

        $leaveSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/09_leave_schema.sql"
            )
        if ($leaveSchemaExitCode -ne 0) {
            throw "Failed to create leave tables."
        }

        $leaveSeedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/10_leave_data.sql"
            )
        if ($leaveSeedExitCode -ne 0) {
            throw "Failed to seed leave data."
        }

        $growthSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/11_growth_schema.sql"
            )
        if ($growthSchemaExitCode -ne 0) {
            throw "Failed to create growth tables."
        }

        $growthSeedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/12_growth_data.sql"
            )
        if ($growthSeedExitCode -ne 0) {
            throw "Failed to seed growth data."
        }

        $interactionSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/13_interaction_schema.sql"
            )
        if ($interactionSchemaExitCode -ne 0) {
            throw "Failed to create interaction tables."
        }

        $interactionSeedExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/14_interaction_data.sql"
            )
        if ($interactionSeedExitCode -ne 0) {
            throw "Failed to seed interaction data."
        }

        $auditSchemaExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "home_school_platform",
                "--execute=source database/15_operation_log_schema.sql"
            )
        if ($auditSchemaExitCode -ne 0) {
            throw "Failed to create operation log table."
        }
    } finally {
        Pop-Location
    }

    $resolvedJavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:JAVA_HOME = $resolvedJavaHome
    $env:DB_URL = "jdbc:mysql://127.0.0.1:$mysqlPort/home_school_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
    $env:DB_USERNAME = "root"
    $env:DB_PASSWORD = ""
    $env:SERVER_PORT = "$apiPort"
    $env:UPLOAD_ROOT = Join-Path $runRoot "uploads"

    $jar = Join-Path $projectRoot "backend\\target\\home-school-platform-0.1.0-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jar)) {
        throw "Backend JAR was not found. Run the full build first."
    }

    $apiProcess = Start-Process `
        -FilePath (Join-Path $resolvedJavaHome "bin\\java.exe") `
        -ArgumentList @("-jar", "`"$jar`"") `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runRoot "api.stdout.log") `
        -RedirectStandardError (Join-Path $runRoot "api.stderr.log") `
        -PassThru

    $baseUrl = "http://127.0.0.1:$apiPort"
    Wait-ForHttp -Uri "$baseUrl/v3/api-docs"

    $credentials = @(
        @{ username = "admin"; password = "Admin@123"; role = "ADMIN"; home = "/admin/home" },
        @{ username = "teacher01"; password = "Teacher@123"; role = "TEACHER"; home = "/teacher/home" },
        @{ username = "student01"; password = "Student@123"; role = "STUDENT"; home = "/student/home" },
        @{ username = "parent01"; password = "Parent@123"; role = "PARENT"; home = "/parent/home" }
    )

    $adminToken = ""
    $teacherToken = ""
    $studentToken = ""
    $parentToken = ""
    foreach ($credential in $credentials) {
        $login = Invoke-JsonPost `
            -Uri "$baseUrl/api/auth/login" `
            -Body @{ username = $credential.username; password = $credential.password }
        if ($login.code -ne 200 -or $login.data.user.roles[0] -ne $credential.role) {
            throw "Login or role verification failed for $($credential.username)."
        }

        $me = Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/auth/me" `
            -Headers @{ Authorization = "Bearer $($login.data.token)" }
        if ($me.code -ne 200 -or $me.data.username -ne $credential.username) {
            throw "Current-user verification failed for $($credential.username)."
        }

        if ($credential.role -eq "ADMIN") {
            $adminToken = $login.data.token
            $logout = Invoke-JsonPost `
                -Uri "$baseUrl/api/auth/logout" `
                -Token $login.data.token `
                -Body @{}
            if ($logout.code -ne 200) {
                throw "Logout verification failed."
            }

            $statusAfterLogout = Get-HttpStatus {
                Invoke-RestMethod `
                    -Method Get `
                    -Uri "$baseUrl/api/auth/me" `
                    -Headers @{ Authorization = "Bearer $($login.data.token)" }
            }
            if ($statusAfterLogout -ne 401) {
                throw "The Token was still valid after logout."
            }
        } elseif ($credential.role -eq "STUDENT") {
            $studentToken = $login.data.token
        } elseif ($credential.role -eq "TEACHER") {
            $teacherToken = $login.data.token
        } elseif ($credential.role -eq "PARENT") {
            $parentToken = $login.data.token
        }

        [pscustomobject]@{
            Username = $credential.username
            Role = $credential.role
            Home = $credential.home
            Result = "PASS"
        } | Format-Table -AutoSize
    }

    $invalidPasswordStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/auth/login" `
            -Body @{ username = "admin"; password = "WrongPassword1" }
    }
    if ($invalidPasswordStatus -ne 401) {
        throw "Invalid credentials did not return HTTP 401."
    }

    $nonAdminStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/admin/users?page=1&size=10" `
            -Headers @{ Authorization = "Bearer $studentToken" }
    }
    if ($nonAdminStatus -ne 403) {
        throw "A student could access an administrator endpoint. HTTP $nonAdminStatus"
    }

    $adminLogin = Invoke-JsonPost `
        -Uri "$baseUrl/api/auth/login" `
        -Body @{ username = "admin"; password = "Admin@123" }
    $adminToken = $adminLogin.data.token
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }

    $roleCount = (Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/roles" `
        -Headers $adminHeaders).data.Count
    if ($roleCount -ne 4) {
        throw "Administrator role listing returned $roleCount roles instead of 4."
    }

    $suffix = "$PID"
    $teacherUser = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/users" `
        -Token $adminToken `
        -Body @{
            username = "teacher_smoke_$suffix"
            password = "SmokeTeacher1"
            realName = "Org Smoke Teacher"
            roleCode = "TEACHER"
            identifier = "TS$suffix"
            gender = "MALE"
            title = "Smoke Title"
            specialty = "Math"
            hireDate = "2026-09-01"
        }
    $studentUser = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/users" `
        -Token $adminToken `
        -Body @{
            username = "student_smoke_$suffix"
            password = "SmokeStudent1"
            realName = "Org Smoke Student"
            roleCode = "STUDENT"
            identifier = "SS$suffix"
            gender = "FEMALE"
            birthDate = "2010-01-01"
            enrollmentDate = "2026-09-01"
        }
    $parentUser = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/users" `
        -Token $adminToken `
        -Body @{
            username = "parent_smoke_$suffix"
            password = "SmokeParent1"
            realName = "Org Smoke Parent"
            roleCode = "PARENT"
            gender = "FEMALE"
            occupation = "Smoke Occupation"
        }

    $teacherProfile = (Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/options/teachers" `
        -Headers $adminHeaders).data |
        Where-Object { $_.label -eq "Org Smoke Teacher" } |
        Select-Object -First 1
    $studentProfile = (Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/options/students" `
        -Headers $adminHeaders).data |
        Where-Object { $_.label -eq "Org Smoke Student" } |
        Select-Object -First 1
    $parentProfile = (Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/options/parents" `
        -Headers $adminHeaders).data |
        Where-Object { $_.label -eq "Org Smoke Parent" } |
        Select-Object -First 1
    if (-not $teacherProfile -or -not $studentProfile -or -not $parentProfile) {
        throw "Created personnel profiles were not returned by option queries."
    }

    $grade = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/grades" `
        -Token $adminToken `
        -Body @{
            gradeName = "Grade2-$suffix"
            gradeLevel = 2
            sortOrder = 2
            status = "ENABLED"
        }
    $class = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/classes" `
        -Token $adminToken `
        -Body @{
            gradeId = $grade.data.id
            className = "TestClass"
            academicYear = "2026-2027"
            graduationYear = 2028
            status = "ENABLED"
        }
    $binding = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/parent-bindings" `
        -Token $adminToken `
        -Body @{
            parentId = $parentProfile.id
            studentId = $studentProfile.id
            relationship = "MOTHER"
            isPrimary = 1
        }
    $assignment = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/teacher-assignments" `
        -Token $adminToken `
        -Body @{
            teacherId = $teacherProfile.id
            classId = $class.data.id
            subjectId = 2
            relationType = "SUBJECT"
            startDate = "2026-09-01"
        }
    $studentClass = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/student-classes" `
        -Token $adminToken `
        -Body @{
            studentId = $studentProfile.id
            classId = $class.data.id
            startDate = "2026-09-01"
        }

    Invoke-JsonPut `
        -Uri "$baseUrl/api/admin/users/$($teacherUser.data.id)" `
        -Token $adminToken `
        -Body @{
            realName = "Org Smoke Teacher Updated"
            phone = ""
            email = ""
        } | Out-Null
    Invoke-JsonPut `
        -Uri "$baseUrl/api/admin/grades/$($grade.data.id)" `
        -Token $adminToken `
        -Body @{
            gradeName = "Grade2Updated-$suffix"
            gradeLevel = 2
            sortOrder = 2
            status = "ENABLED"
        } | Out-Null
    Invoke-JsonPut `
        -Uri "$baseUrl/api/admin/classes/$($class.data.id)" `
        -Token $adminToken `
        -Body @{
            gradeId = $grade.data.id
            className = "TestClassUpdated"
            academicYear = "2026-2027"
            graduationYear = 2028
            status = "ENABLED"
        } | Out-Null

    $deleteGrade = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/grades" `
        -Token $adminToken `
        -Body @{
            gradeName = "DeleteGrade-$suffix"
            gradeLevel = 3
            sortOrder = 3
            status = "ENABLED"
        }
    $deleteClass = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/classes" `
        -Token $adminToken `
        -Body @{
            gradeId = $deleteGrade.data.id
            className = "DeleteClass"
            academicYear = "2026-2027"
            status = "ENABLED"
        }
    Invoke-Delete `
        -Uri "$baseUrl/api/admin/classes/$($deleteClass.data.id)" `
        -Token $adminToken | Out-Null
    Invoke-Delete `
        -Uri "$baseUrl/api/admin/grades/$($deleteGrade.data.id)" `
        -Token $adminToken | Out-Null
    $missingCreatedId = (
        -not $teacherUser.data.id -or
        -not $studentUser.data.id -or
        -not $parentUser.data.id -or
        -not $class.data.id -or
        -not $binding.data.id -or
        -not $assignment.data.id -or
        -not $studentClass.data.id
    )
    if ($missingCreatedId) {
        throw "One or more organization creation calls did not return an ID."
    }

    $teacherPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/users?page=1&size=20&roleCode=TEACHER&keyword=$suffix" `
        -Headers $adminHeaders
    $bindingPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/parent-bindings?page=1&size=20&keyword=$suffix" `
        -Headers $adminHeaders
    $assignmentPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/teacher-assignments?page=1&size=20&keyword=$suffix" `
        -Headers $adminHeaders
    $studentClassPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/student-classes?page=1&size=20&keyword=$suffix" `
        -Headers $adminHeaders
    $missingPageData = (
        $teacherPage.data.total -lt 1 -or
        $bindingPage.data.total -lt 1 -or
        $assignmentPage.data.total -lt 1 -or
        $studentClassPage.data.total -lt 1
    )
    if ($missingPageData) {
        throw "One or more organization query pages did not return the created data."
    }

    Invoke-JsonPatch `
        -Uri "$baseUrl/api/admin/parent-bindings/$($binding.data.id)/status" `
        -Token $adminToken `
        -Body @{ status = "REVOKED" } | Out-Null
    Invoke-RestMethod `
        -Method Patch `
        -Uri "$baseUrl/api/admin/teacher-assignments/$($assignment.data.id)/end" `
        -Headers $adminHeaders | Out-Null

    $uploadSource = Join-Path $runRoot "notice-smoke.txt"
    [IO.File]::WriteAllText($uploadSource, "notice smoke attachment", [Text.Encoding]::UTF8)
    $uploadRaw = & curl.exe `
        --silent `
        --show-error `
        --request POST `
        --header "Authorization: Bearer $adminToken" `
        --form "file=@$uploadSource;type=text/plain" `
        "$baseUrl/api/files"
    if ($LASTEXITCODE -ne 0) {
        throw "Attachment upload request failed."
    }
    $upload = $uploadRaw | ConvertFrom-Json
    if ($upload.code -ne 200 -or -not $upload.data.id) {
        throw "Attachment upload did not return an attachment ID."
    }
    $tempDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $studentToken" `
        "$baseUrl/api/files/$($upload.data.id)/download"
    if ($tempDownloadStatus -ne "403") {
        throw "An unbound temporary attachment was downloadable by another user."
    }

    $draftNotice = Invoke-JsonPost `
        -Uri "$baseUrl/api/notices" `
        -Token $adminToken `
        -Body @{
            title = "Smoke school notice $suffix"
            content = "Published notice content for smoke testing."
            scopeType = "ALL"
            attachmentIds = @($upload.data.id)
        }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/notices/$($draftNotice.data.id)/publish" `
        -Token $adminToken `
        -Body @{} | Out-Null
    $gradeNotice = Invoke-JsonPost `
        -Uri "$baseUrl/api/notices" `
        -Token $adminToken `
        -Body @{
            title = "Smoke grade notice $suffix"
            content = "Published grade notice content for smoke testing."
            scopeType = "GRADE"
            gradeId = 1
        }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/notices/$($gradeNotice.data.id)/publish" `
        -Token $adminToken `
        -Body @{} | Out-Null

    $studentNotices = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/notices?page=1&size=20&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    if ($studentNotices.data.total -lt 2) {
        throw "Published ALL or GRADE notices were not visible to the student."
    }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/notices/$($draftNotice.data.id)/read" `
        -Token $studentToken `
        -Body @{} | Out-Null
    Invoke-JsonPost `
        -Uri "$baseUrl/api/notices/$($draftNotice.data.id)/read" `
        -Token $studentToken `
        -Body @{} | Out-Null
    $noticeReaders = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/notices/$($draftNotice.data.id)/readers?page=1&size=20" `
        -Headers $adminHeaders
    if ($noticeReaders.data.total -ne 1) {
        throw "Idempotent notice read test failed."
    }

    $downloadPath = Join-Path $runRoot "downloaded-notice.txt"
    $downloadStatus = & curl.exe `
        --silent `
        --output $downloadPath `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $studentToken" `
        "$baseUrl/api/files/$($upload.data.id)/download"
    if ($downloadStatus -ne "200" -or -not (Test-Path -LiteralPath $downloadPath)) {
        throw "Authorized attachment download failed."
    }

    $teacherNotice = Invoke-JsonPost `
        -Uri "$baseUrl/api/notices" `
        -Token $teacherToken `
        -Body @{
            title = "Smoke class notice $suffix"
            content = "Class notice visible to class students and parents."
            scopeType = "CLASS"
            classId = 1
        }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/notices/$($teacherNotice.data.id)/publish" `
        -Token $teacherToken `
        -Body @{} | Out-Null
    $parentNotices = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/notices?page=1&size=20&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    if ($parentNotices.data.total -lt 1) {
        throw "A class notice was not visible to a bound parent."
    }
    $teacherAllNoticeStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/notices" `
            -Token $teacherToken `
            -Body @{
                title = "Forbidden all notice"
                content = "This request must be rejected."
                scopeType = "ALL"
            }
    }
    if ($teacherAllNoticeStatus -ne 403) {
        throw "A teacher could publish an ALL notice."
    }

    Invoke-JsonPost `
        -Uri "$baseUrl/api/notices/$($draftNotice.data.id)/withdraw" `
        -Token $adminToken `
        -Body @{} | Out-Null
    $withdrawnDetailStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/notices/$($draftNotice.data.id)" `
            -Headers @{ Authorization = "Bearer $studentToken" }
    }
    $withdrawnDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $studentToken" `
        "$baseUrl/api/files/$($upload.data.id)/download"
    if ($withdrawnDetailStatus -ne 403 -or $withdrawnDownloadStatus -ne "403") {
        throw "Withdrawn notice visibility checks failed."
    }

    $deadline = (Get-Date).AddDays(30).ToString("yyyy-MM-ddTHH:mm:ss")
    $assignmentUploadSource = Join-Path $runRoot "assignment-smoke.txt"
    [IO.File]::WriteAllText(
        $assignmentUploadSource,
        "assignment smoke attachment",
        [Text.Encoding]::UTF8)
    $assignmentUploadRaw = & curl.exe `
        --silent `
        --show-error `
        --request POST `
        --header "Authorization: Bearer $teacherToken" `
        --form "file=@$assignmentUploadSource;type=text/plain" `
        "$baseUrl/api/files"
    if ($LASTEXITCODE -ne 0) {
        throw "Assignment attachment upload failed."
    }
    $assignmentUpload = $assignmentUploadRaw | ConvertFrom-Json
    $assignmentDraft = Invoke-JsonPost `
        -Uri "$baseUrl/api/assignments" `
        -Token $teacherToken `
        -Body @{
            title = "AssignmentSmoke$suffix"
            content = "Complete the smoke assignment and submit a file."
            classId = 1
            subjectId = 2
            deadline = $deadline
            maxScore = 100
            attachmentIds = @($assignmentUpload.data.id)
        }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)/publish" `
        -Token $teacherToken `
        -Body @{} | Out-Null

    $studentAssignment = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    if ($studentAssignment.data.submissionStatus -ne "UNSUBMITTED") {
        throw "A new assignment was not initially unsubmitted."
    }
    $assignmentDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $studentToken" `
        "$baseUrl/api/files/$($assignmentUpload.data.id)/download"
    if ($assignmentDownloadStatus -ne "200") {
        throw "An authorized student could not download the assignment attachment."
    }

    $submissionUploadSource = Join-Path $runRoot "submission-smoke.txt"
    [IO.File]::WriteAllText(
        $submissionUploadSource,
        "student submission attachment",
        [Text.Encoding]::UTF8)
    $submissionUploadRaw = & curl.exe `
        --silent `
        --show-error `
        --request POST `
        --header "Authorization: Bearer $studentToken" `
        --form "file=@$submissionUploadSource;type=text/plain" `
        "$baseUrl/api/files"
    if ($LASTEXITCODE -ne 0) {
        throw "Student submission attachment upload failed."
    }
    $submissionUpload = $submissionUploadRaw | ConvertFrom-Json
    $submissionResult = Invoke-JsonPost `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)/submission" `
        -Token $studentToken `
        -Body @{
            content = "Student smoke submission."
            attachmentIds = @($submissionUpload.data.id)
        }
    $submissionId = $submissionResult.data.submissionId

    $submissionPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)/submissions?page=1&size=20" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    if ($submissionPage.data.total -lt 1) {
        throw "Teacher submission list did not return the submitted student."
    }
    $parentSubmissionDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $parentToken" `
        "$baseUrl/api/files/$($submissionUpload.data.id)/download"
    if ($parentSubmissionDownloadStatus -ne "200") {
        throw "A bound parent could not download the child's submission attachment."
    }

    $overMaxGradeStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/submissions/$submissionId/grade" `
            -Token $teacherToken `
            -Body @{ score = 101; comment = "Invalid score" }
    }
    if ($overMaxGradeStatus -ne 400) {
        throw "An over-max assignment score was not rejected."
    }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/submissions/$submissionId/grade" `
        -Token $teacherToken `
        -Body @{ score = 95; comment = "Well done." } | Out-Null
    $gradedDetail = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    $gradeCheckFailed = (
        $gradedDetail.data.submissionStatus -ne "GRADED" -or
        [decimal]$gradedDetail.data.score -ne 95
    )
    if ($gradeCheckFailed) {
        throw "Student graded assignment result was incorrect."
    }
    $parentAssignments = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/assignments?page=1&size=20&studentId=1&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    $parentAssignmentCheckFailed = (
        $parentAssignments.data.total -lt 1 -or
        $parentAssignments.data.records[0].submissionStatus -ne "GRADED"
    )
    if ($parentAssignmentCheckFailed) {
        throw "Parent assignment completion view failed."
    }
    $unboundChildStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/assignments?page=1&size=20&studentId=999" `
            -Headers @{ Authorization = "Bearer $parentToken" }
    }
    if ($unboundChildStatus -ne 403) {
        throw "A parent could query an unbound child's assignments."
    }

    Invoke-JsonPost `
        -Uri "$baseUrl/api/submissions/$submissionId/return" `
        -Token $teacherToken `
        -Body @{ reason = "Please add more detail." } | Out-Null
    Invoke-JsonPost `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)/submission" `
        -Token $studentToken `
        -Body @{
            content = "Student resubmission with more detail."
            attachmentIds = @($submissionUpload.data.id)
        } | Out-Null
    $resubmittedDetail = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/assignments/$($assignmentDraft.data.id)" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    $resubmissionCheckFailed = (
        $resubmittedDetail.data.submissionStatus -ne "SUBMITTED" -or
        $resubmittedDetail.data.attemptCount -ne 2
    )
    if ($resubmissionCheckFailed) {
        throw "Returned assignment resubmission failed."
    }

    $teacherOutsideClassStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/assignments" `
            -Token $teacherToken `
            -Body @{
                title = "Forbidden class assignment"
                content = "This assignment must be rejected."
                classId = $class.data.id
                subjectId = 2
                deadline = $deadline
                maxScore = 100
            }
    }
    if ($teacherOutsideClassStatus -ne 403) {
        throw "A teacher could publish an assignment to an untaught class."
    }

    $examPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/exams?page=1&size=20" `
        -Headers $adminHeaders
    if ($examPage.data.total -lt 1) {
        throw "Examination page did not return seeded data."
    }

    $smokeSubject = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/subjects" `
        -Token $adminToken `
        -Body @{
            subjectCode = "SMOKE$suffix"
            subjectName = "Smoke Subject $suffix"
            defaultFullScore = 100
            sortOrder = 99
            status = "ENABLED"
        }
    Invoke-JsonPut `
        -Uri "$baseUrl/api/admin/subjects/$($smokeSubject.data.id)" `
        -Token $adminToken `
        -Body @{
            subjectCode = "SMOKE$suffix"
            subjectName = "Smoke Subject Updated $suffix"
            defaultFullScore = 100
            sortOrder = 99
            status = "ENABLED"
        } | Out-Null
    Invoke-Delete `
        -Uri "$baseUrl/api/admin/subjects/$($smokeSubject.data.id)" `
        -Token $adminToken | Out-Null

    $smokeExam = Invoke-JsonPost `
        -Uri "$baseUrl/api/admin/exams" `
        -Token $adminToken `
        -Body @{
            name = "ScoreExam$suffix"
            examType = "QUIZ"
            gradeId = 1
            classId = 1
            academicYear = "2026-2027"
            term = "Term1"
            examDate = "2026-11-20"
            status = "PUBLISHED"
        }
    $workspaceOptions = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/scores/workspace-options" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    $matchingExam = $workspaceOptions.data.exams |
        Where-Object { $_.id -eq $smokeExam.data.id } |
        Select-Object -First 1
    if (-not $matchingExam) {
        throw "Teacher score workspace did not include the assigned-class exam."
    }

    $scoreBatch = @{
        examId = $smokeExam.data.id
        classId = 1
        subjectId = 2
        records = @(
            @{
                studentId = 1
                score = 130
                absent = $false
                remark = "First score"
            }
        )
    }
    Invoke-JsonPut `
        -Uri "$baseUrl/api/scores/batch" `
        -Token $teacherToken `
        -Body $scoreBatch | Out-Null
    $scoreBatch.records[0].score = 135
    $scoreBatch.records[0].remark = "Updated score"
    Invoke-JsonPut `
        -Uri "$baseUrl/api/scores/batch" `
        -Token $teacherToken `
        -Body $scoreBatch | Out-Null

    $entryRows = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/scores/entry?examId=$($smokeExam.data.id)&classId=1&subjectId=2" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    $studentScore = $entryRows.data |
        Where-Object { $_.studentId -eq 1 } |
        Select-Object -First 1
    if (-not $studentScore -or [decimal]$studentScore.score -ne 135) {
        throw "Score unique upsert did not preserve the latest value."
    }

    $statistics = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/scores/statistics?examId=$($smokeExam.data.id)&classId=1&subjectId=2" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    if ([decimal]$statistics.data.averageScore -ne 135) {
        throw "Class score statistics were incorrect."
    }

    $invalidScoreStatus = Get-HttpStatus {
        Invoke-JsonPut `
            -Uri "$baseUrl/api/scores/batch" `
            -Token $teacherToken `
            -Body @{
                examId = $smokeExam.data.id
                classId = 1
                subjectId = 2
                records = @(
                    @{
                        studentId = 1
                        score = 151
                        absent = $false
                    }
                )
            }
    }
    if ($invalidScoreStatus -ne 400) {
        throw "A score above the subject full score was not rejected."
    }
    $untaughtSubjectStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/scores/entry?examId=$($smokeExam.data.id)&classId=1&subjectId=1" `
            -Headers @{ Authorization = "Bearer $teacherToken" }
    }
    if ($untaughtSubjectStatus -ne 403) {
        throw "A teacher accessed score entry for an untaught subject."
    }

    $studentScoreReport = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/scores/report" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    $reportedScore = $studentScoreReport.data.items |
        Where-Object { $_.examId -eq $smokeExam.data.id -and $_.subjectId -eq 2 } |
        Select-Object -First 1
    if (-not $reportedScore -or [decimal]$reportedScore.score -ne 135) {
        throw "Student score report did not contain the saved score."
    }
    $parentScoreReport = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/scores/report?studentId=1" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    $parentScoreReportFailed = (
        $parentScoreReport.data.studentId -ne 1 -or
        $parentScoreReport.data.items.Count -lt 1
    )
    if ($parentScoreReportFailed) {
        throw "Parent score report failed."
    }
    $unboundScoreReportStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/scores/report?studentId=999" `
            -Headers @{ Authorization = "Bearer $parentToken" }
    }
    if ($unboundScoreReportStatus -ne 403) {
        throw "A parent accessed an unbound child's score report."
    }

    $deleteExamWithScoreStatus = Get-HttpStatus {
        Invoke-Delete `
            -Uri "$baseUrl/api/admin/exams/$($smokeExam.data.id)" `
            -Token $adminToken
    }
    if ($deleteExamWithScoreStatus -ne 409) {
        throw "An examination with scores could be deleted."
    }

    $leaveStart = (Get-Date).Date.AddDays(60).AddHours(8)
    $leaveEnd = $leaveStart.AddHours(8)
    $leaveProofSource = Join-Path $runRoot "leave-proof-smoke.txt"
    [IO.File]::WriteAllText(
        $leaveProofSource,
        "leave proof attachment",
        [Text.Encoding]::UTF8)
    $leaveProofRaw = & curl.exe `
        --silent `
        --show-error `
        --request POST `
        --header "Authorization: Bearer $studentToken" `
        --form "file=@$leaveProofSource;type=text/plain" `
        "$baseUrl/api/files"
    $leaveProof = $leaveProofRaw | ConvertFrom-Json
    $leaveCreateBody = @{
        leaveType = "SICK"
        startAt = $leaveStart.ToString("yyyy-MM-ddTHH:mm:ss")
        endAt = $leaveEnd.ToString("yyyy-MM-ddTHH:mm:ss")
        reason = "Smoke leave request"
        attachmentIds = @($leaveProof.data.id)
    }
    $createdLeave = Invoke-JsonPost `
        -Uri "$baseUrl/api/leaves" `
        -Token $studentToken `
        -Body $leaveCreateBody
    $overlapLeaveStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/leaves" `
            -Token $studentToken `
            -Body @{
                leaveType = "OTHER"
                startAt = $leaveStart.AddMinutes(30).ToString("yyyy-MM-ddTHH:mm:ss")
                endAt = $leaveEnd.AddMinutes(30).ToString("yyyy-MM-ddTHH:mm:ss")
                reason = "Overlapping request"
            }
    }
    if ($overlapLeaveStatus -ne 409) {
        throw "An overlapping leave request was not rejected."
    }

    $leaveApprovalPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/leaves/approval?page=1&size=20&status=PENDING" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    $createdLeaveVisible = $leaveApprovalPage.data.records |
        Where-Object { $_.id -eq $createdLeave.data.id } |
        Select-Object -First 1
    if (-not $createdLeaveVisible) {
        throw "Homeroom teacher approval list did not contain the pending leave."
    }

    $otherTeacherLogin = Invoke-JsonPost `
        -Uri "$baseUrl/api/auth/login" `
        -Body @{
            username = "teacher_smoke_$suffix"
            password = "SmokeTeacher1"
        }
    $otherTeacherToken = $otherTeacherLogin.data.token
    $otherTeacherApprovalStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/leaves/$($createdLeave.data.id)/decision" `
            -Token $otherTeacherToken `
            -Body @{
                decision = "APPROVED"
                opinion = "Should be rejected"
            }
    }
    if ($otherTeacherApprovalStatus -ne 403) {
        throw "A non-homeroom teacher approved a leave request."
    }

    Invoke-JsonPost `
        -Uri "$baseUrl/api/leaves/$($createdLeave.data.id)/decision" `
        -Token $teacherToken `
        -Body @{
            decision = "APPROVED"
            opinion = "Approved for medical treatment."
        } | Out-Null
    $repeatDecisionStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/leaves/$($createdLeave.data.id)/decision" `
            -Token $teacherToken `
            -Body @{
                decision = "REJECTED"
                opinion = "Duplicate decision"
            }
    }
    if ($repeatDecisionStatus -ne 409) {
        throw "A leave request could be approved twice."
    }

    $studentLeaveDetail = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/leaves/$($createdLeave.data.id)" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    if ($studentLeaveDetail.data.status -ne "APPROVED") {
        throw "Student could not see the approved leave status."
    }
    $parentLeavePage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/leaves?page=1&size=20&studentId=1" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    if ($parentLeavePage.data.total -lt 1) {
        throw "Parent leave history was empty."
    }
    $leaveProofDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $parentToken" `
        "$baseUrl/api/files/$($leaveProof.data.id)/download"
    if ($leaveProofDownloadStatus -ne "200") {
        throw "Bound parent could not download leave proof."
    }

    $secondLeaveStart = $leaveStart.AddDays(2)
    $secondLeaveEnd = $secondLeaveStart.AddHours(2)
    $secondLeave = Invoke-JsonPost `
        -Uri "$baseUrl/api/leaves" `
        -Token $studentToken `
        -Body @{
            leaveType = "PERSONAL"
            startAt = $secondLeaveStart.ToString("yyyy-MM-ddTHH:mm:ss")
            endAt = $secondLeaveEnd.ToString("yyyy-MM-ddTHH:mm:ss")
            reason = "Leave to cancel"
        }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/leaves/$($secondLeave.data.id)/cancel" `
        -Token $studentToken `
        -Body @{} | Out-Null
    $adminLeavePage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/leaves?page=1&size=50" `
        -Headers $adminHeaders
    if ($adminLeavePage.data.total -lt 2) {
        throw "Administrator leave record list failed."
    }

    $growthVisibleProofSource = Join-Path $runRoot "growth-visible-smoke.txt"
    [IO.File]::WriteAllText(
        $growthVisibleProofSource,
        "growth visible attachment",
        [Text.Encoding]::UTF8)
    $growthVisibleProofRaw = & curl.exe `
        --silent `
        --show-error `
        --request POST `
        --header "Authorization: Bearer $teacherToken" `
        --form "file=@$growthVisibleProofSource;type=text/plain" `
        "$baseUrl/api/files"
    $growthVisibleProof = $growthVisibleProofRaw | ConvertFrom-Json
    $visibleGrowth = Invoke-JsonPost `
        -Uri "$baseUrl/api/growth-records" `
        -Token $teacherToken `
        -Body @{
            studentId = 1
            category = "HONOR"
            title = "GrowthVisible$suffix"
            content = "Visible growth record for student and parent."
            occurredAt = "2026-11-20"
            visibleToParent = $true
            attachmentIds = @($growthVisibleProof.data.id)
        }

    $growthPrivateProofSource = Join-Path $runRoot "growth-private-smoke.txt"
    [IO.File]::WriteAllText(
        $growthPrivateProofSource,
        "growth private attachment",
        [Text.Encoding]::UTF8)
    $growthPrivateProofRaw = & curl.exe `
        --silent `
        --show-error `
        --request POST `
        --header "Authorization: Bearer $teacherToken" `
        --form "file=@$growthPrivateProofSource;type=text/plain" `
        "$baseUrl/api/files"
    $growthPrivateProof = $growthPrivateProofRaw | ConvertFrom-Json
    $privateGrowth = Invoke-JsonPost `
        -Uri "$baseUrl/api/growth-records" `
        -Token $teacherToken `
        -Body @{
            studentId = 1
            category = "COMPREHENSIVE"
            title = "GrowthPrivate$suffix"
            content = "Internal homeroom growth note."
            occurredAt = "2026-11-21"
            visibleToParent = $false
            attachmentIds = @($growthPrivateProof.data.id)
        }

    $nonHomeroomGrowthStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/growth-records" `
            -Token $otherTeacherToken `
            -Body @{
                studentId = 1
                category = "STUDY"
                title = "ForbiddenGrowth$suffix"
                content = "This must be rejected."
                occurredAt = "2026-11-20"
                visibleToParent = $true
            }
    }
    if ($nonHomeroomGrowthStatus -ne 403) {
        throw "A non-homeroom teacher created a growth record."
    }

    $teacherGrowthPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/growth-records/manage?page=1&size=20&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    if ($teacherGrowthPage.data.total -lt 2) {
        throw "Homeroom teacher growth management list failed."
    }
    $studentGrowthPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/growth-records?page=1&size=20&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $studentToken" }
    if ($studentGrowthPage.data.total -ne 2) {
        throw "Student growth archive did not contain both records."
    }
    $parentGrowthPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/growth-records?page=1&size=20&studentId=1&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    if ($parentGrowthPage.data.total -ne 1) {
        throw "Parent growth archive visibility filter failed."
    }
    $visibleGrowthDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $parentToken" `
        "$baseUrl/api/files/$($growthVisibleProof.data.id)/download"
    $privateGrowthDownloadStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Authorization: Bearer $parentToken" `
        "$baseUrl/api/files/$($growthPrivateProof.data.id)/download"
    if ($visibleGrowthDownloadStatus -ne "200" -or $privateGrowthDownloadStatus -ne "403") {
        throw "Growth attachment visibility checks failed."
    }

    Invoke-JsonPut `
        -Uri "$baseUrl/api/growth-records/$($visibleGrowth.data.id)" `
        -Token $teacherToken `
        -Body @{
            studentId = 1
            category = "HONOR"
            title = "GrowthVisibleUpdated$suffix"
            content = "Updated visible growth record."
            occurredAt = "2026-11-20"
            visibleToParent = $true
            attachmentIds = @($growthVisibleProof.data.id)
        } | Out-Null
    $disposableGrowth = Invoke-JsonPost `
        -Uri "$baseUrl/api/growth-records" `
        -Token $teacherToken `
        -Body @{
            studentId = 1
            category = "ACTIVITY"
            title = "GrowthDelete$suffix"
            content = "Disposable growth record."
            occurredAt = "2026-11-22"
            visibleToParent = $true
        }
    Invoke-Delete `
        -Uri "$baseUrl/api/growth-records/$($disposableGrowth.data.id)" `
        -Token $teacherToken | Out-Null
    $adminGrowthPage = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/growth-records?page=1&size=50&keyword=$suffix" `
        -Headers $adminHeaders
    if ($adminGrowthPage.data.total -lt 2) {
        throw "Administrator growth archive list failed."
    }

    $parentTeacherOptions = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/messages/options/teachers?studentId=1" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    if ($parentTeacherOptions.data.Count -lt 1) {
        throw "Parent message teacher options were empty."
    }
    $studentMessage = Invoke-JsonPost `
        -Uri "$baseUrl/api/messages" `
        -Token $studentToken `
        -Body @{
            studentId = 1
            teacherId = 1
            subject = "StudentMessage$suffix"
            content = "Student interaction smoke message."
        }
    $parentMessage = Invoke-JsonPost `
        -Uri "$baseUrl/api/messages" `
        -Token $parentToken `
        -Body @{
            studentId = 1
            teacherId = 1
            subject = "ParentMessage$suffix"
            content = "Parent interaction smoke message."
        }
    $invalidTeacherMessageStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/messages" `
            -Token $studentToken `
            -Body @{
                studentId = 1
                teacherId = $teacherProfile.id
                subject = "InvalidTeacher$suffix"
                content = "This teacher does not teach the current class."
            }
    }
    if ($invalidTeacherMessageStatus -ne 403) {
        throw "A student could message a teacher outside the current class."
    }
    $unboundParentMessageStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/messages" `
            -Token $parentToken `
            -Body @{
                studentId = 999
                teacherId = 1
                subject = "UnboundChild$suffix"
                content = "Must be rejected."
            }
    }
    if ($unboundParentMessageStatus -ne 403) {
        throw "A parent sent a message for an unbound child."
    }

    $teacherInbox = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/messages/received?page=1&size=30&keyword=$suffix" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    if ($teacherInbox.data.total -lt 2) {
        throw "Teacher inbox did not contain both new messages."
    }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/messages/$($parentMessage.data.id)/read" `
        -Token $teacherToken `
        -Body @{} | Out-Null
    Invoke-JsonPost `
        -Uri "$baseUrl/api/messages/$($parentMessage.data.id)/replies" `
        -Token $teacherToken `
        -Body @{ content = "Teacher reply for parent smoke test." } | Out-Null
    $parentThread = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/messages/$($parentMessage.data.id)" `
        -Headers @{ Authorization = "Bearer $parentToken" }
    $parentThreadFailed = (
        $parentThread.data.message.status -ne "REPLIED" -or
        $parentThread.data.replies.Count -lt 1
    )
    if ($parentThreadFailed) {
        throw "Parent could not see the teacher reply."
    }
    Invoke-JsonPost `
        -Uri "$baseUrl/api/messages/$($parentMessage.data.id)/replies" `
        -Token $parentToken `
        -Body @{ content = "Parent follow-up reply." } | Out-Null
    $teacherThread = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/messages/$($parentMessage.data.id)" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    if ($teacherThread.data.message.status -ne "UNREAD") {
        throw "Initiator follow-up did not mark the conversation unread."
    }
    $nonParticipantMessageStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/messages/$($parentMessage.data.id)" `
            -Headers @{ Authorization = "Bearer $otherTeacherToken" }
    }
    if ($nonParticipantMessageStatus -ne 403) {
        throw "A non-participant teacher accessed a message thread."
    }

    Invoke-JsonPost `
        -Uri "$baseUrl/api/messages/$($parentMessage.data.id)/close" `
        -Token $teacherToken `
        -Body @{} | Out-Null
    $closedReplyStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/messages/$($parentMessage.data.id)/replies" `
            -Token $parentToken `
            -Body @{ content = "Reply after close must fail." }
    }
    if ($closedReplyStatus -ne 409) {
        throw "A closed message accepted a new reply."
    }
    $adminMessages = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/messages/received?page=1&size=50&keyword=$suffix" `
        -Headers $adminHeaders
    if ($adminMessages.data.total -lt 2) {
        throw "Administrator message audit list failed."
    }

    $adminDashboard = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/dashboard/admin" `
        -Headers $adminHeaders
    $adminDashboardFailed = (
        $adminDashboard.data.studentCount -lt 1 -or
        $adminDashboard.data.teacherCount -lt 1 -or
        $adminDashboard.data.parentCount -lt 1 -or
        $adminDashboard.data.classCount -lt 1 -or
        $adminDashboard.data.publishedNoticeCount -lt 1
    )
    if ($adminDashboardFailed) {
        throw "Administrator dashboard statistics were incorrect."
    }
    $teacherDashboard = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/dashboard/teacher" `
        -Headers @{ Authorization = "Bearer $teacherToken" }
    $teacherDashboardFailed = (
        $teacherDashboard.data.classCount -lt 1 -or
        $teacherDashboard.data.studentCount -lt 1 -or
        $teacherDashboard.data.publishedAssignmentCount -lt 1 -or
        $teacherDashboard.data.pendingGradingCount -lt 1 -or
        $teacherDashboard.data.pendingLeaveCount -lt 1
    )
    if ($teacherDashboardFailed) {
        throw "Teacher dashboard statistics were incorrect."
    }
    $studentDashboardStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/dashboard/admin" `
            -Headers @{ Authorization = "Bearer $studentToken" }
    }
    if ($studentDashboardStatus -ne 403) {
        throw "A student accessed the administrator dashboard."
    }
    $operationLogs = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/operation-logs?page=1&size=50" `
        -Headers $adminHeaders
    if ($operationLogs.data.total -lt 20) {
        throw "Operation log audit records were incomplete."
    }
    $failedOperationLogs = Invoke-RestMethod `
        -Method Get `
        -Uri "$baseUrl/api/admin/operation-logs?page=1&size=20&resultCode=409" `
        -Headers $adminHeaders
    if ($failedOperationLogs.data.total -lt 1) {
        throw "Operation log result-code filtering failed."
    }
    $studentAuditStatus = Get-HttpStatus {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$baseUrl/api/admin/operation-logs?page=1&size=10" `
            -Headers @{ Authorization = "Bearer $studentToken" }
    }
    if ($studentAuditStatus -ne 403) {
        throw "A student accessed operation logs."
    }

    Invoke-JsonPatch `
        -Uri "$baseUrl/api/admin/users/$($teacherUser.data.id)/status" `
        -Token $adminToken `
        -Body @{ status = "DISABLED" } | Out-Null
    $disabledLoginStatus = Get-HttpStatus {
        Invoke-JsonPost `
            -Uri "$baseUrl/api/auth/login" `
            -Body @{
                username = "teacher_smoke_$suffix"
                password = "SmokeTeacher1"
            }
    }
    if ($disabledLoginStatus -ne 401) {
        throw "A disabled teacher account could still log in."
    }

    Push-Location $projectRoot
    try {
        $initAllExitCode = Invoke-NativeCommand `
            -FilePath $mysql `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "--default-character-set=utf8mb4",
                "--execute=source database/init_all.sql"
            )
        if ($initAllExitCode -ne 0) {
            throw "Unified database initialization script failed."
        }
    } finally {
        Pop-Location
    }

    [pscustomobject]@{
        MySQL = "8.0.35 isolated instance"
        LoginRoles = "4/4 PASS"
        CurrentUser = "4/4 PASS"
        LogoutTokenRevocation = "PASS"
        InvalidPassword = "HTTP 401 PASS"
        Swagger = "/v3/api-docs PASS"
        OrganizationCRUD = "PASS"
        NoticeWorkflow = "PASS"
        NoticeScopeAuthorization = "PASS"
        NoticeGradeScope = "PASS"
        NoticeReadIdempotency = "PASS"
        AttachmentAccessControl = "PASS"
        AssignmentWorkflow = "PASS"
        AssignmentSubmission = "PASS"
        AssignmentGradingReturn = "PASS"
        ParentAssignmentScope = "PASS"
        ExamAndSubjectCRUD = "PASS"
        ScoreBatchUpsert = "PASS"
        ScoreRangeAuthorization = "PASS"
        ScoreReportAndParentScope = "PASS"
        LeaveWorkflow = "PASS"
        LeaveOverlapConcurrency = "PASS"
        HomeroomAuthorization = "PASS"
        LeaveCancelAndParentScope = "PASS"
        GrowthArchiveWorkflow = "PASS"
        GrowthParentVisibility = "PASS"
        GrowthHomeroomAuthorization = "PASS"
        GrowthAttachmentControl = "PASS"
        InteractionWorkflow = "PASS"
        InteractionParticipantScope = "PASS"
        InteractionReplyLifecycle = "PASS"
        InteractionAudit = "PASS"
        AdminDashboardStatistics = "PASS"
        TeacherDashboardStatistics = "PASS"
        DashboardAuthorization = "PASS"
        OperationLogAudit = "PASS"
        OperationLogAuthorization = "PASS"
        DatabaseInitialization = "PASS"
        AdministratorAuthorization = "HTTP 403 for student PASS"
        DisabledAccount = "HTTP 401 PASS"
    } | Format-List
} finally {
    if ($apiProcess -and -not $apiProcess.HasExited) {
        Stop-Process -Id $apiProcess.Id -Force
        $apiProcess.WaitForExit(10000) | Out-Null
    }

    if (Test-Path -LiteralPath $mysqladmin) {
        Invoke-NativeCommand `
            -FilePath $mysqladmin `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "shutdown"
            ) | Out-Null
    }
    if ($mysqlProcess -and -not $mysqlProcess.HasExited) {
        $mysqlProcess.WaitForExit(10000) | Out-Null
        if (-not $mysqlProcess.HasExited) {
            Stop-Process -Id $mysqlProcess.Id -Force
        }
    }

    if (Test-Path -LiteralPath $runRoot) {
        $resolvedRunRoot = (Resolve-Path -LiteralPath $runRoot).Path
        $tempPrefix = $tempBase.TrimEnd("\") + "\"
        if (-not $resolvedRunRoot.StartsWith($tempPrefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a smoke-test directory outside the temporary directory: $resolvedRunRoot"
        }
        Remove-Item -LiteralPath $resolvedRunRoot -Recurse -Force
    }
}

