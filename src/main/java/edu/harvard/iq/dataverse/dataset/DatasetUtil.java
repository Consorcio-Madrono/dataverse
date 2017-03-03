package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class DatasetUtil {

    private static final Logger logger = Logger.getLogger(DatasetUtil.class.getCanonicalName());
    public static String datasetLogoFilenameFinal = "dataset_logo_final";
    public static String datasetLogoFilenameStaging = "dataset_logo_staging";
    public static String stagingFilePathKey = "stagingFilePath";
    public static String stagingFileErrorKey = "error";

    public static List<DatasetThumbnail> getThumbnailCandidates(Dataset dataset, boolean considerDatasetLogoAsCandidate) {
        List<DatasetThumbnail> thumbnails = new ArrayList<>();
        if (dataset == null) {
            return thumbnails;
        }
        if (considerDatasetLogoAsCandidate) {
            Path datasetLogo = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoFilenameFinal);
            if (Files.exists(datasetLogo)) {
                File file = datasetLogo.toFile();
                String imageSourceBase64 = null;
                try {
                    imageSourceBase64 = FileUtil.rescaleImage(file);
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, null);
                    thumbnails.add(datasetThumbnail);
                } catch (IOException ex) {
                    logger.info("Unable to rescale image: " + ex);
                }
            }
        }
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            if (dataFile != null && dataFile.isImage()) {
                String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(dataFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, dataFile);
                thumbnails.add(datasetThumbnail);
            }
        }
        return thumbnails;
    }

    public static DatasetThumbnail getThumbnail(Dataset dataset, DatasetVersionServiceBean datasetVersionService, DataFileServiceBean dataFileService) {
        if (dataset == null) {
            return null;
        }
        String title = dataset.getLatestVersion().getTitle();
        Path datasetLogo = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoFilenameFinal);
        if (Files.exists(datasetLogo)) {
            File file = datasetLogo.toFile();
            String imageSourceBase64 = null;
            try {
                imageSourceBase64 = FileUtil.rescaleImage(file);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, null);
                logger.info(title + " will get thumbnail from dataset logo.");
                return datasetThumbnail;
            } catch (IOException ex) {
                logger.info("Unable to rescale image: " + ex);
                return null;
            }
        } else {
            DataFile thumbnailFile = dataset.getThumbnailFile();
            if (thumbnailFile == null) {
                logger.fine(title + " does not have a thumbnail file set but the search card might have one");
                DatasetThumbnail thumbnailThatMightBeOnSearchCard = findThumbnailThatMightBeShowingOnTheSearchCards(dataset, datasetVersionService, dataFileService);
                if (thumbnailThatMightBeOnSearchCard != null) {
                    logger.fine(title + " does not have a thumbnail file set but a thumbnail was found as a search card thumbnail.");
                    return thumbnailThatMightBeOnSearchCard;
                } else {
                    logger.info(title + " does not have a thumbnail file set but and couldn't find one in use on the search card.");
                    // returning null because dataFile.equals(thumbnailFile) will never match since thumbnailFile is null and there's no point in interating through the files
                    return null;
                }
            }
            String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(thumbnailFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, thumbnailFile);
            logger.fine(title + " will get thumbnail from DataFile id " + thumbnailFile.getId());
            return datasetThumbnail;
        }
    }

    public static DatasetThumbnail findThumbnailThatMightBeShowingOnTheSearchCards(Dataset dataset, DatasetVersionServiceBean datasetVersionService, DataFileServiceBean dataFileService) {
        boolean disableThisMethodToSeeIfWeCanDeleteIt = false;
        if (disableThisMethodToSeeIfWeCanDeleteIt) {
            return null;
        }
        if (dataset == null) {
            logger.info("Dataset is null so returning null.");
            return null;
        }
        if (datasetVersionService == null || dataFileService == null) {
            logger.info("Without service beans, can't determine if search cards have a thumbnail or not for dataset id " + dataset.getId());
            return null;
        }
        Long randomThumbnail = datasetVersionService.getThumbnailByVersionId(dataset.getLatestVersion().getId());
        if (randomThumbnail != null) {
            DataFile thumbnailImageFile = null;
            thumbnailImageFile = dataFileService.findCheapAndEasy(randomThumbnail);
            if (dataFileService.isThumbnailAvailable(thumbnailImageFile)) {
                String randomlySelectedThumbnail = ImageThumbConverter.getImageThumbAsBase64(
                        thumbnailImageFile,
                        ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(randomlySelectedThumbnail, thumbnailImageFile);
                return datasetThumbnail;
            }
        }
        return null;
    }

    public static JsonObjectBuilder writeDatasetLogoToStagingArea(Dataset dataset, File file) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        if (dataset == null) {
            jsonObjectBuilder.add(stagingFileErrorKey, "Dataset is null.");
            return jsonObjectBuilder;
        }
        if (file == null) {
            jsonObjectBuilder.add(stagingFileErrorKey, "Dataset is id " + dataset.getId() + " but file is null.");
            return jsonObjectBuilder;
        }
        try {
            if (dataset.getFileSystemDirectory() != null && !Files.exists(dataset.getFileSystemDirectory())) {
                /**
                 * Note that "createDirectories()" must be used - not
                 * "createDirectory()", to make sure all the parent directories
                 * that may not yet exist are created as well.
                 */
                Path directoryCreated = Files.createDirectories(dataset.getFileSystemDirectory());
                logger.fine("Dataset directory created: " + directoryCreated);
            }
        } catch (IOException ex) {
            String msg = "Failed to create dataset directory " + dataset.getFileSystemDirectory() + " - " + ex;
            logger.severe(msg);
            jsonObjectBuilder.add(stagingFileErrorKey, msg);
            return jsonObjectBuilder;
        }
        File newFile = null;
        String stagingFilePath = null;
        try {
            newFile = File.createTempFile(datasetLogoFilenameStaging, ".png");
            stagingFilePath = newFile.toPath().toString();
            // goes to some place like this: /var/folders/c2/qts2_6zn7cl5h8g8x7r7xcbr0000gn/T/dataset_logo_staging5070333955726753809.png
            logger.fine("Uploaded file written to staging area: " + newFile.getAbsolutePath());
            jsonObjectBuilder.add(stagingFilePathKey, stagingFilePath);
        } catch (IOException ex) {
            Logger.getLogger(DatasetUtil.class.getName()).log(Level.SEVERE, null, ex);
            jsonObjectBuilder.add(stagingFileErrorKey, "Problem creating temp file: " + ex);
            return jsonObjectBuilder;
        }

        try {
            Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return jsonObjectBuilder;
        } catch (IOException ex) {
            String msg = "Failed to copy file to " + newFile.toPath() + ": " + ex;
            logger.severe(msg);
            jsonObjectBuilder.add(stagingFileErrorKey, msg);
            return jsonObjectBuilder;
        }
    }

    public static Dataset moveDatasetLogoFromStagingToFinal(Dataset dataset, String stagingFilePath) {
        if (dataset == null) {
            return null;
        }
        File stagingFile = new File(stagingFilePath);
        File finalFile = new File(dataset.getFileSystemDirectory().toString(), datasetLogoFilenameFinal);
        try {
            Files.copy(stagingFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.severe("Failed to copy file from " + stagingFile.getAbsolutePath() + " to " + finalFile.getAbsolutePath() + ": " + ex);
        }
        boolean stagingFileDeleted = stagingFile.delete();
        return dataset;
    }

    public static boolean deleteDatasetLogo(Dataset dataset) {
        File doomed = new File(dataset.getFileSystemDirectory().toString(), datasetLogoFilenameFinal);
        return doomed.delete();
    }

}
