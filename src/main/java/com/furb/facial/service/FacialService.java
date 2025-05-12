package com.furb.facial.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.Attribute;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.CompareFacesMatch;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.ComparedFace;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;
import software.amazon.awssdk.services.rekognition.model.Emotion;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FacialService {
    private final RekognitionClient rekognitionClient;

    public FacialService(RekognitionClient rekognitionClient) {
        this.rekognitionClient = rekognitionClient;
    }

//   ->> REMOVE AZURE SERVICE, NO TIME TO WAIT THEIR PERMISSION TO GET OTHER INFORMATIONS OF RESPONSE'S API <<-
//    public void getInfoFacial() {
//        String endpoint = "";
//        String apiKey = "";
//        String imageUrl = "FOTO_SORRINDO.jpg";
//
//        // Cria o cliente da API
//        FaceClient client = new FaceClientBuilder()
//                .endpoint(endpoint)
//                .credential(new AzureKeyCredential(apiKey))
//                .buildClient();
//
//        // Configura os atributos a serem recuperados
//        List<FaceAttributeType> atributos = Arrays.asList(
//                FaceAttributeType.AGE,
//                FaceAttributeType.GLASSES,
//                FaceAttributeType.FACIAL_HAIR,
//                FaceAttributeType.HEAD_POSE,
//                FaceAttributeType.QUALITY_FOR_RECOGNITION,
//                FaceAttributeType.OCCLUSION,
//                FaceAttributeType.EXPOSURE,
//                FaceAttributeType.NOISE,
//                FaceAttributeType.BLUR
//        );
//
//        // Opções de detecção
//        DetectOptions options = new DetectOptions(FaceDetectionModel.DETECTION_03, FaceRecognitionModel.RECOGNITION_04, false);
//        try {
//            // Detecta rostos na imagem
//            List<FaceDetectionResult> rostos = client.detect(imageUrl, options);
//
//            if (rostos.isEmpty()) {
//                System.out.println("Nenhum rosto detectado.");
//                return;
//            }
//
//            // Processa cada rosto detectado
//            for (FaceDetectionResult rosto : rostos) {
//                System.out.println("======== DETALHES DO ROSTO ========");
//
//                // Geometria
//                FaceRectangle retangulo = rosto.getFaceRectangle();
//                System.out.printf("Posição: [Top: %d, Left: %d, Width: %d, Height: %d]%n",
//                        retangulo.getTop(), retangulo.getLeft(), retangulo.getWidth(), retangulo.getHeight());
//
//                // Atributos
//                FaceAttributes atributosRosto = rosto.getFaceAttributes();
//                System.out.printf("Idade estimada: %.1f anos%n", atributosRosto.getAge());
//                System.out.println("Óculos: " + atributosRosto.getGlasses());
//
//                // Detalhes faciais
//                FacialHair peloFacial = atributosRosto.getFacialHair();
//                System.out.printf("Barba: %.2f%% | Bigode: %.2f%% | Costeletas: %.2f%%%n",
//                        peloFacial.getBeard(), peloFacial.getMoustache(), peloFacial.getSideburns());
//
//                // Qualidade técnica
//                System.out.println("Qualidade para reconhecimento: " + atributosRosto.getQualityForRecognition());
//                System.out.printf("Oclusão: Sobrancelhas=%s, Olhos=%s, Boca=%s%n",
//                        atributosRosto.getOcclusion().isEyeOccluded(),
//                        atributosRosto.getOcclusion().isForeheadOccluded(),
//                        atributosRosto.getOcclusion().isMouthOccluded());
//
//                System.out.printf("Exposição: %s | Ruído: %s | Desfoque: %s%n",
//                        atributosRosto.getExposure().getValue(),
//                        atributosRosto.getNoise().getValue(),
//                        atributosRosto.getBlur().getValue());
//
//                System.out.println("===================================");
//            }
//
//        } catch (Exception e) {
//            System.err.println("Erro na chamada da API: " + e.getMessage());
//        }
//    }

    public void getFacialInformationByAws() {

        SdkBytes finalByteImage = getSdkBytesByImage("img/eu_no_escuro.jpg");
        DetectFacesRequest request = DetectFacesRequest.builder()
                .image(i -> i.bytes(finalByteImage))
                .attributes(Attribute.ALL)
                .build();

        DetectFacesResponse result = rekognitionClient.detectFaces(request);

        for (FaceDetail face : result.faceDetails()) {
            System.out.println("Idade estimada: " + face.ageRange().low() + " - " + face.ageRange().high());
            System.out.println("Gênero: " + face.gender().value());
            System.out.println("Sorrindo: " + face.smile().value());
            System.out.println("Emoções:");
            for (Emotion emotion : face.emotions()) {
                System.out.println(" - " + emotion.type() + ": " + emotion.confidence());
            }
            System.out.println("----------");
        }

        rekognitionClient.close();
    }

    public void comparingFacialImages() {
        SdkBytes finalImageOne = getSdkBytesByImage("img/eu_normal.jpg");
        SdkBytes finalImageTwo = getSdkBytesByImage("img/eu_no_escuro.jpg");

        CompareFacesRequest requestComparingFaces =
                CompareFacesRequest.builder()
                        .sourceImage(b -> b.bytes(finalImageOne).build())
                        .targetImage(t -> t.bytes(finalImageTwo).build())
                        .build();

        CompareFacesResponse result = rekognitionClient.compareFaces(requestComparingFaces);

        for (CompareFacesMatch match : result.faceMatches()) {
            ComparedFace face = match.face();
            BoundingBox box = face.boundingBox();
            System.out.printf("Rosto correspondente encontrado na posição (esquerda: %.2f, topo: %.2f) com %.2f%% de confiança.%n",
                    box.left(), box.top(), face.confidence());
            System.out.printf("Similaridade com a imagem de origem: %.2f%%%n", match.similarity());
        }

        // Exibir rostos não correspondentes
        List<ComparedFace> unmatchedFaces = result.unmatchedFaces();
        System.out.printf("Número de rostos não correspondentes: %d%n", unmatchedFaces.size());

        // Corrigindo orientação, se necessário
        if (result.sourceImageOrientationCorrection() != null) {
            System.out.println("Correção de orientação da imagem de origem: " + result.sourceImageOrientationCorrection());
        }
        if (result.targetImageOrientationCorrection() != null) {
            System.out.println("Correção de orientação da imagem de destino: " + result.targetImageOrientationCorrection());
        }

        rekognitionClient.close();
    }

    private static SdkBytes getSdkBytesByImage(String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            return SdkBytes.fromByteArray(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler a imagem: " + imagePath, e);
        }
    }

}
