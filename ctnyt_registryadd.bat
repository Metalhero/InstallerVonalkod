reg add HKEY_CLASSES_ROOT\CTNYT /t REG_SZ /d "CT nyomtatvany kezelo protocol" /f
reg add HKEY_CLASSES_ROOT\CTNYT /v "URL Protocol" /t REG_SZ /d "" /f
reg add HKEY_CLASSES_ROOT\CTNYT\shell /f
reg add HKEY_CLASSES_ROOT\CTNYT\shell\open /f
reg add HKEY_CLASSES_ROOT\CTNYT\shell\open\command /t REG_SZ /d "\"c:\cttemp\CTNYT.bat\" \"%%1"" /f
exit