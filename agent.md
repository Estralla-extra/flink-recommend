# Agent 操作规范

## 项目编码规范

本项目运行在 Windows 中文系统 + PowerShell 5 环境下。
所有源文件必须使用 UTF-8 无 BOM 编码。

---

## 1. 环境陷阱

- PowerShell 5 默认编码是 GBK (ANSI 936)，不是 UTF-8
- `Set-Content` 不指定编码 => 写 GBK
- `Set-Content -Encoding UTF8` => 写 UTF-8 但带 BOM
- BOM 头部 (EF BB BF) 会让 Java 编译器报错
- `Get-Content` 显示中文正常不等于文件是 UTF-8 (因为它在 GBK 解码)

## 2. 唯一安全的写入方法

使用 .NET 直接写入 UTF-8 无 BOM:

```powershell
$enc = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText("file.java", "内容", $enc)
```

不能使用:
- `Set-Content "file.java" -Value "中文"`
- `@'...'@ | Set-Content`
- 任何涉及 PowerShell 管道的写入

## 3. 检测文件编码

```powershell
$bytes = [System.IO.File]::ReadAllBytes("File.java")
$text = [System.Text.Encoding]::UTF8.GetString($bytes)
# 如果中文显示为乱码（如 "鍏滃簳"），文件是 GBK
```

## 4. 修复 GBK 文件

```powershell
$enc = New-Object System.Text.UTF8Encoding $false
$gbk = [System.Text.Encoding]::GetEncoding("GBK")
$bytes = [System.IO.File]::ReadAllBytes("File.java")
$correct = $gbk.GetString($bytes)
[System.IO.File]::WriteAllText("File.java", $correct, $enc)
```

## 5. 检查清单

创建/修改文件后:
- 用 .NET 的 `ReadAllBytes + UTF8.GetString` 验证过源文件中文
- .java 文件没有 BOM（前 3 字节不是 EF BB BF）
- Vue 文件用 `npx vite build` 确认构建成功
- 构建产物用 `Select-String "词语" built.js` 验证中文正确