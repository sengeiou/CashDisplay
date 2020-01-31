package com.resonance.cashdisplay.load;

import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_SUCCESSFULL;

public class DownloadResult {
    public int hasError = DOWNLOAD_RESULT_SUCCESSFULL;
    public long countFiles = 0;
    public long countSkipped = 0;
    public long countDeleted = 0;
}