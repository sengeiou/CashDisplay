package com.resonance.cashdisplay.load;

import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SUCCESSFULL;

public class UploadResult {
    public int hasError = UPLOAD_RESULT_SUCCESSFULL;
    public long countFiles = 0;
    public long countSkipped = 0;
    public long countDeleted = 0;
}