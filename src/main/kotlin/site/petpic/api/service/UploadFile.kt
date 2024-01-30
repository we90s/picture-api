package site.petpic.api.service


import com.google.cloud.storage.StorageOptions
import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString


class UploadFile {

    fun uploadGCS(byte:ByteArray, userMail : String): String{
        // val labels = AtomicReference("")
        val storage = StorageOptions.getDefaultInstance().service
        val originKey = "origin/$userMail${System.currentTimeMillis()}.png"


        val bucket = storage.get("petpicbucket") ?: error("bucketName does not exist.")


        bucket.create(originKey, byte, "image/jpeg")
        storage.close()
        return originKey
    }
    fun callVisionAPI(byte:ByteArray): String? {
        val imgBytes: ByteString = ByteString.copyFrom(byte)
        val vision = ImageAnnotatorClient.create()
        val requests: MutableList<AnnotateImageRequest> = ArrayList()
        val img: Image = Image.newBuilder().setContent(imgBytes).build()
        val feat: Feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build()
        val request: AnnotateImageRequest =
            AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build()
        requests.add(request)

        val response: BatchAnnotateImagesResponse = vision.batchAnnotateImages(requests)

        val responses: List<AnnotateImageResponse> = response.responsesList

        if(responses.first().error.code == 3){
            return null
        }
        //     val animal =  responses.first().labelAnnotationsList.first().description


//        val descriptions = mutableListOf<String>()
//
//
//        for (response in responses) {
//            val annotations = response.labelAnnotationsList
//            val max = min(annotations.size, 3)
//            for (i in 0 until max) {
//                descriptions.add(annotations[i].description)
//            }
//        }
//        println(descriptions)
        return responses.first().labelAnnotationsList.first().description
    }

}