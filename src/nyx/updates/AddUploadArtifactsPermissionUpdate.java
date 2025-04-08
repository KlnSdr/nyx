package nyx.updates;

import hades.authorized.Group;
import hades.authorized.Permission;
import hades.authorized.service.GroupService;
import hades.update.Update;
import hades.user.User;
import hades.user.service.UserService;

public class AddUploadArtifactsPermissionUpdate implements Update {
    @Override
    public boolean run() {
        final User[] adminRead = UserService.getInstance().findByName("admin");

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


        if (!GroupService.getInstance().update(group)) {
            return false;
        }

        return GroupService.getInstance().addUserToGroup(admin.getId().toString(), group.getKey());
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
