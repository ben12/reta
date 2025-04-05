@SET mypath=%~dp0
@start javaw -cp "%mypath:~0,-1%\libs\*" com.ben12.reta.Launcher %1
