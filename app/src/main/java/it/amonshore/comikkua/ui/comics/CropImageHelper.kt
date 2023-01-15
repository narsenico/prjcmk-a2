package it.amonshore.comikkua.ui.comics

import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

fun createCropImageContractOptions(): CropImageContractOptions {
    return CropImageContractOptions(
        uri = null,
        cropImageOptions = CropImageOptions(
            guidelines = CropImageView.Guidelines.ON,
            cropShape = CropImageView.CropShape.OVAL,
            fixAspectRatio = true,
        )
    )
}