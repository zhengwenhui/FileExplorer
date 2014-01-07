package com.android.file.explorer;

interface ICopyService
{
    void testFun(String path);
    void startCopy(String targetDir, boolean is_move);
    void setCopyStatus(int status);
    int getCopyStatus();
    void notificaFromActivity(boolean isrestart);
}
