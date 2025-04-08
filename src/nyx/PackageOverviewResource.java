package nyx;

import common.html.*;
import common.logger.Logger;
import dobby.Config;
import dobby.annotations.Get;
import dobby.io.HttpContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PackageOverviewResource {
    private static final Logger LOGGER = new Logger(PackageOverviewResource.class);

    @Get("/")
    public void getPackageOverview(HttpContext context) {
        final List<String> files = findFiles().stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[0], files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get("/{group}")
    public void getPackageOverviewForGroup(HttpContext context) {
        final String group = context.getRequest().getParam("group");

        final List<String> files = findFiles(group).stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[]{group}, files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get("/{group}/{name}")
    public void getPackageOverviewForGroupAndName(HttpContext context) {
        final String group = context.getRequest().getParam("group");
        final String name = context.getRequest().getParam("name");

        final List<String> files = findFiles(group, name).stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[]{group, name}, files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get("/{group}/{name}/{version}")
    public void getPackageOverviewForGroupNameAndVersion(HttpContext context) {
        final String group = context.getRequest().getParam("group");
        final String name = context.getRequest().getParam("name");
        final String version = context.getRequest().getParam("version");

        final List<String> files = findFiles(group, name, version).stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[]{group, name, version}, files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    private Document buildUi(String[] currentLoc, List<String> files) {
        final Document doc = new Document();
        doc.setTitle("nyx repo" + (currentLoc.length > 0 ? " - " + String.join(":", currentLoc) : ""));

        final Div navbar = new Div();
        navbar.addChild(new Link("/", "Home"));

        for (int i = 0; i < currentLoc.length; i++) {
            navbar.addChild(new Label(" / "));
            navbar.addChild(new Link("../".repeat(currentLoc.length - i - 1), currentLoc[i]));
        }
        doc.addChild(navbar);

        if (files.isEmpty()) {
            doc.addChild(new Paragraph("Failed to find source files"));
            return doc;
        }

        final Ul artefactList = new Ul();
        doc.addChild(artefactList);

        Collections.sort(files);

        for (String file : files) {
            final Li li = new Li("");
            li.addChild(new Link(file + "/", file));
            artefactList.addChild(li);
        }

        return doc;
    }

    private List<String> findFiles() {
        return findFilesRelativeToRoot("");
    }

    private List<String> findFiles(String group) {
        return findFilesRelativeToRoot("/" + group);
    }

    private List<String> findFiles(String group, String name) {
        return findFilesRelativeToRoot("/" + group + "/" + name);
    }

    private List<String> findFiles(String group, String name, String version) {
        return findFilesRelativeToRoot("/" + group + "/" + name + "/" + version);
    }

    private List<String> findFilesRelativeToRoot(String srcDirExtension) {
        final String srcDir = Config.getInstance().getString("dobby.staticContent.externalDocRoot") + srcDirExtension;
        try {
            return Files.walk(Paths.get(srcDir)).map(Path::toString).filter(path -> path.endsWith(".jar") || path.endsWith(".json")).map(path -> path.replace(srcDir, "")).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Failed to find source files");
            LOGGER.trace(e);
            return List.of();
        }
    }
}
