package com.furb.facial.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.Attribute;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.Celebrity;
import software.amazon.awssdk.services.rekognition.model.CompareFacesMatch;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.ComparedFace;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Emotion;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesRequest;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesResponse;

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

    /**
     * Mostrar informações da face e emoções.
     */
    public void getFacialInformationByAws() {

        SdkBytes finalByteImage = getSdkBytesByImage("img/nojinho.jpg");
        DetectFacesRequest request = DetectFacesRequest.builder()
                .image(i -> i.bytes(finalByteImage))
                .attributes(Attribute.ALL)
                .build();

        DetectFacesResponse result = rekognitionClient.detectFaces(request);

        for (FaceDetail face : result.faceDetails()) {
            System.out.println("Idade estimada: " + face.ageRange().low() + " - " + face.ageRange().high());
            System.out.println("Gênero: " + face.gender().value());
            System.out.println("Sorrindo: " + face.smile().value());
            System.out.println("Olho esta aberto? : " + face.eyesOpen());
            System.out.println("Emoções:");
            for (Emotion emotion : face.emotions()) {
                System.out.println(" - " + emotion.type() + ": " + emotion.confidence());
            }
            System.out.println("----------");
        }

        rekognitionClient.close();
    }

    /**
     * Comparação de faces.
     */
    public void comparingFacialImages() {
        SdkBytes finalImageOne = getSdkBytesByImage("img/eu_normal.jpg");
        SdkBytes finalImageTwo = getSdkBytesByImage("img/daniel.jpg");

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

    /**
     * Identificação de celebridades.
     */
    public void recognizeCelebritiesFromImage() {
        SdkBytes imageBytes = getSdkBytesByImage("img/thaina_celebridade.jpg");

        RecognizeCelebritiesRequest request = RecognizeCelebritiesRequest.builder()
                .image(i -> i.bytes(imageBytes))
                .build();

        RecognizeCelebritiesResponse response = rekognitionClient.recognizeCelebrities(request);

        List<Celebrity> celebs = response.celebrityFaces();
        if (celebs.isEmpty()) {
            System.out.println("Nenhuma celebridade reconhecida.");
        } else {
            for (Celebrity celeb : celebs) {
                System.out.println("Nome da celebridade: " + celeb.name());
                System.out.println("Confiança: " + celeb.matchConfidence());
                System.out.println("Links: " + celeb.urls());
                System.out.println("----");
            }
        }

        rekognitionClient.close();
    }

    /**
     * Identificação de objetos/coisas impróprias.
     */
    public void detectModerationLabels() {
        SdkBytes imageBytes = getSdkBytesByImage("img/cigarro.jpg");

        DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                .image(i -> i.bytes(imageBytes))
                .minConfidence(75F)
                .build();

        DetectModerationLabelsResponse response = rekognitionClient.detectModerationLabels(request);

        System.out.println("Moderação encontrada:");
        for (ModerationLabel label : response.moderationLabels()) {
            System.out.printf("- %s (confiança: %.2f%%)%n", label.name(), label.confidence());
        }

        rekognitionClient.close();
    }

    /**
     * Identificação de objetos.
     */
    public void detectLabels() {
        SdkBytes imageBytes = getSdkBytesByImage("img/objetos2.jpg");

        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(i -> i.bytes(imageBytes))
                .maxLabels(20)
                .minConfidence(75F)
                .build();

        DetectLabelsResponse response = rekognitionClient.detectLabels(request);

        System.out.println("Rótulos detectados:");
        for (Label label : response.labels()) {
            System.out.printf("- %s (confiança: %.2f%%)%n", label.name(), label.confidence());
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
