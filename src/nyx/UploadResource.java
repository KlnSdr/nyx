package nyx;

import common.logger.Logger;
import dobby.Config;
import dobby.annotations.Post;
import dobby.io.HttpContext;
import dobby.io.response.ResponseCodes;
import dobby.util.json.NewJson;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class UploadResource {
    private static final String BASE_PATH = "/rest/upload";
    private static final Logger LOGGER = new Logger(UploadResource.class);
    private static final String srcDir = Config.getInstance().getString("dobby.staticContent.externalDocRoot");

    @Post(BASE_PATH)
    public void uploadArtifact(HttpContext context) {
        final NewJson body = context.getRequest().getBody();

        if (body == null || !body.hasKeys("group", "name", "version")) {
            context.getResponse().setCode(ResponseCodes.BAD_REQUEST);
            context.getResponse().setBody("Invalid request body");
            return;
        }

        final Map<String, File> files = context.getRequest().getFiles();

        if (files.isEmpty() || !files.containsKey("artifact") || !files.containsKey("nyxjson")) {
            context.getResponse().setCode(ResponseCodes.BAD_REQUEST);
            context.getResponse().setBody("No files uploaded");
            return;
        }

        final String group = body.getString("group");
        final String name = body.getString("name");
        final String version = body.getString("version");

        if (!createDirIfNotExists(srcDir + "/" + group + "/" + name + "/" + version)) {
            context.getResponse().setCode(ResponseCodes.INTERNAL_SERVER_ERROR);
            context.getResponse().setBody("Failed to create group directory");
            return;
        }

        final File artifactFile = files.get("artifact");
        final File nyxJsonFile = files.get("nyxjson");

        final String artifactPath = srcDir + "/" + group + "/" + name + "/" + version + "/" + name + ".jar";
        final String nyxJsonPath = srcDir + "/" + group + "/" + name + "/" + version + "/nyx.json";
        final File artifactDest = new File(artifactPath);
        final File nyxJsonDest = new File(nyxJsonPath);

        if (artifactDest.exists() || nyxJsonDest.exists()) {
            if (!artifactDest.delete()) {
                context.getResponse().setCode(ResponseCodes.INTERNAL_SERVER_ERROR);
                context.getResponse().setBody("Failed to delete existing artifact file");
                return;
            }
            if (!nyxJsonDest.delete()) {
                context.getResponse().setCode(ResponseCodes.INTERNAL_SERVER_ERROR);
                context.getResponse().setBody("Failed to delete existing nyx.json file");
                return;
            }
        }

        try {

            if (!artifactFile.renameTo(artifactDest)) {
                Files.move(artifactFile.toPath(), artifactDest.toPath());
            }
            if (!nyxJsonFile.renameTo(nyxJsonDest)) {
                Files.move(nyxJsonFile.toPath(), nyxJsonDest.toPath());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to move files");
            LOGGER.trace(e);
            context.getResponse().setCode(ResponseCodes.INTERNAL_SERVER_ERROR);
            context.getResponse().setBody("Failed to move files");
        }
    }

    private boolean createDirIfNotExists(String path) {
        final File dir = new File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
}
