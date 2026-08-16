Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
strPath = fso.GetParentFolderName(WScript.ScriptFullName)

' Try pythonw.exe first, fallback to pyw.exe if needed
Dim pyExec
pyExec = "pythonw.exe"

On Error Resume Next
WshShell.Run pyExec & " """ & strPath & "\cli_launcher.py"" --background", 0, False
If Err.Number <> 0 Then
    Err.Clear
    pyExec = "pyw.exe"
    WshShell.Run pyExec & " """ & strPath & "\cli_launcher.py"" --background", 0, False
End If
