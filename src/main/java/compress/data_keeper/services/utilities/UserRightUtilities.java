package compress.data_keeper.services.utilities;

import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.forbidden.exceptions.UserDoesntHaveRightException;
import compress.data_keeper.security.contstants.Role;

public class UserRightUtilities {

    public static void checkUserRights(Folder folder, User user) {
        if (user.getRole().equals(Role.ROLE_ADMIN)) {
            return;
        }
        if (!folder.getOwner().equals(user)) {
            throw new UserDoesntHaveRightException(user.getEmail());
        }
    }

    public static void checkUserRights(Long userId, User user) {
        if (user.getRole().equals(Role.ROLE_ADMIN)) {
            return;
        }
        if (!userId.equals(user.getId())) {
            throw new UserDoesntHaveRightException(user.getEmail());
        }
    }
}
