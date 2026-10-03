package nyx;

import common.html.*;
import common.inject.api.Inject;
import common.inject.api.RegisterFor;
import common.logger.Logger;
import dobby.IConfig;
import dobby.annotations.Get;
import dobby.exceptions.MalformedJsonException;
import dobby.io.HttpContext;
import dobby.io.response.ResponseCodes;
import dobby.util.json.NewJson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RegisterFor(PackageOverviewResource.class)
public class PackageOverviewResource {
    private static final Logger LOGGER = new Logger(PackageOverviewResource.class);
    private static final String BASE_PATH = "/packages";
    private final IConfig config;

    @Inject
    public PackageOverviewResource(IConfig config) {
        this.config = config;
    }

    @Get("/")
    public void index(HttpContext context) {
        context.getResponse().setHeader("Location", BASE_PATH + "/");
        context.getResponse().setCode(ResponseCodes.PERMANENT_REDIRECT);
    }

    @Get(BASE_PATH)
    public void getPackageOverview(HttpContext context) {
        final List<String> files = findFiles().stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[0], files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get(BASE_PATH + "/{group}")
    public void getPackageOverviewForGroup(HttpContext context) {
        final String group = context.getRequest().getParam("group");

        final List<String> files = findFiles(group).stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[]{group}, files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get(BASE_PATH + "/{group}/{name}")
    public void getPackageOverviewForGroupAndName(HttpContext context) {
        final String group = context.getRequest().getParam("group");
        final String name = context.getRequest().getParam("name");

        final List<String> files = findFiles(group, name).stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[]{group, name}, files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get(BASE_PATH + "/{group}/{name}/{version}")
    public void getPackageOverviewForGroupNameAndVersion(HttpContext context) {
        final String group = context.getRequest().getParam("group");
        final String name = context.getRequest().getParam("name");
        final String version = context.getRequest().getParam("version");

        final List<String> files = findFiles(group, name, version).stream().map(file -> file.split("/")[1]).distinct().collect(Collectors.toList());
        context.getResponse().setBody(buildUi(new String[]{group, name, version}, files).toHtml());
        context.getResponse().setHeader("Content-Type", "text/html");
    }

    @Get(BASE_PATH + "/{group}/{name}/{version}/{file}")
    public void redirectToFile(HttpContext context) {
        final String group = context.getRequest().getParam("group");
        final String name = context.getRequest().getParam("name");
        final String version = context.getRequest().getParam("version");
        final String file = context.getRequest().getParam("file");

        context.getResponse().setHeader("Location", "/" + group + "/" + name + "/" + version + "/" + file);
        context.getResponse().setCode(ResponseCodes.PERMANENT_REDIRECT);
    }

    private Document buildUi(String[] currentLoc, List<String> files) {
        final Document doc = new Document();
        doc.setTitle("nyx repo" + (currentLoc.length > 0 ? " - " + String.join(":", currentLoc) : ""));

        final Div navbar = new Div();
        navbar.addChild(new Link(BASE_PATH + "/", "Home"));

        for (int i = 0; i < currentLoc.length; i++) {
            navbar.addChild(new Label(" / "));
            navbar.addChild(new Link("../".repeat(currentLoc.length - i - 1), currentLoc[i]));
        }
        doc.addChild(navbar);

        if (files.isEmpty()) {
            doc.addChild(new Paragraph("Failed to find source files"));
            return doc;
        }

        if (currentLoc.length > 2) {
            Headline headline = new Headline(1, currentLoc[1] + " (v" + currentLoc[2] + ")");
            doc.addChild(headline);
        }
        if (currentLoc.length == 3) {
            final Headline h2 = new Headline(2, "Artifacts");
            doc.addChild(h2);
        }

        final Ul artefactList = new Ul();
        doc.addChild(artefactList);

        Collections.sort(files);

        for (String file : files) {
            final Li li = new Li("");
            li.addChild(new Link(file + "/", file));
            artefactList.addChild(li);
        }

        if (currentLoc.length == 3 && files.contains("nyx.json")) {
            final Headline h2 = new Headline(2, "Dependencies");
            doc.addChild(h2);

            final File nyxJsonFile = new File(config.getString("dobby.staticContent.externalDocRoot") + "/" + currentLoc[0] + "/" + currentLoc[1] + "/" + currentLoc[2] + "/nyx.json");
            if (nyxJsonFile.exists() && nyxJsonFile.isFile()) {
                renderDependencies(doc, nyxJsonFile);
            } else {
                doc.addChild(new Paragraph("nyx.json file not found"));
            }
        }

        return doc;
    }

    private void renderDependencies(Document doc, File nyxJsonFile) {
        try {
            final String nyxJsonContent = Files.readString(nyxJsonFile.toPath());
            final NewJson nyxJson = NewJson.parse(nyxJsonContent);
            final List<Object> dependencies = nyxJson.getList("project.dependencies");
            if (dependencies == null || dependencies.isEmpty() || dependencies.stream().allMatch(dep -> dep instanceof NewJson && ((NewJson) dep).getString("group") == null && ((NewJson) dep).getString("name") == null && ((NewJson) dep).getString("version") == null)) {
                doc.addChild(new Paragraph("No dependencies found."));
                return;
            }
            final List<NewJson> jsonDependencies = dependencies.stream().filter(dep -> dep instanceof NewJson).map(dep -> (NewJson) dep).collect(Collectors.toList());

            final Ul dependencyList = new Ul();
            for (NewJson dependency : jsonDependencies) {
                final String group = dependency.getString("group");
                final String name = dependency.getString("name");
                final String version = dependency.getString("version");
                if (group != null && name != null && version != null) {
                    final Li li = new Li("");
                    li.addChild(new Link(BASE_PATH + "/" + group + "/" + name + "/" + version, group + ":" + name + ":" + version));
                    dependencyList.addChild(li);
                }
            }
            doc.addChild(dependencyList);
        } catch (IOException | MalformedJsonException e) {
            doc.addChild(new Paragraph("Failed to read dependency information."));
            LOGGER.trace(e);
        }
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
        final String srcDir = config.getString("dobby.staticContent.externalDocRoot") + srcDirExtension;
        try {
            return Files.walk(Paths.get(srcDir)).map(Path::toString).filter(path -> path.endsWith(".jar") || path.endsWith(".json")).map(path -> path.replace(srcDir, "")).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Failed to find source files");
            LOGGER.trace(e);
            return List.of();
        }
    }
}
