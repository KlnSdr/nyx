package nyx.updates;

import common.inject.api.Inject;
import common.inject.api.RegisterFor;
import hades.authorized.Group;
import hades.authorized.Permission;
import hades.authorized.service.GroupService;
import hades.update.Update;
import hades.user.User;
import hades.user.service.UserService;

@RegisterFor(AddUploadArtifactsPermissionUpdate.class)
public class AddUploadArtifactsPermissionUpdate implements Update {
    private final UserService userService;
    private final GroupService groupService;

    @Inject
    public AddUploadArtifactsPermissionUpdate(UserService userService, GroupService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

    @Override
    public boolean run() {
        final User[] adminRead = userService.findByName("admin");

        if (adminRead.length == 0) {
            return false;
        }
        final User admin = adminRead[0];

        final Group group = new Group("nyx-upload-artifacts");

        final Permission permission = new Permission();
        permission.setOwner(group.getId());
        permission.setRoute("/rest/upload");
        permission.setPermissionGET(false);
        permission.setPermissionPOST(true);
        permission.setPermissionPUT(false);
        permission.setPermissionDELETE(false);

        group.addPermission(permission);


        if (!groupService.update(group)) {
            return false;
        }

        return groupService.addUserToGroup(admin.getId().toString(), group.getKey());
    }

    @Override
    public String getName() {
        return "AddUploadArtifactsPermissionUpdate";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
