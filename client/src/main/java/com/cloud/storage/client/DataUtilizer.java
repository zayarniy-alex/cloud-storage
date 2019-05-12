package com.cloud.storage.client;

import com.cloud.storage.common.AbstractMessage;

public interface DataUtilizer {
    void utilizeFileList(AbstractMessage data);
    void authFailed();
    void authOk();
    void uploadFinished();
    void utilizeFile(AbstractMessage data);
    void deauth();
}
