package com.resonance.cashdisplay.load;

import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_SUCCESSFULL;

public class DownloadResult {
    public int HasError = DOWNLOAD_RESULT_SUCCESSFULL;
    public long CountFiles = 0;
    public long CountSkiped = 0;
    public long CountDeleted = 0;
}