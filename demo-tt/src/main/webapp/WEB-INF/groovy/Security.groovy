//import com.google.appengine.api.users.User


//import com.manydesigns.portofino.shiro.GAEPortofinoRealm


import com.manydesigns.elements.ElementsThreadLocals
import com.manydesigns.elements.messages.SessionMessages
import com.manydesigns.elements.reflection.ClassAccessor
import com.manydesigns.elements.util.RandomUtil
import com.manydesigns.mail.stripes.SendMailAction
import com.manydesigns.portofino.di.Inject
import com.manydesigns.portofino.logic.SecurityLogic
import com.manydesigns.portofino.model.database.Database
import com.manydesigns.portofino.model.database.DatabaseLogic
import com.manydesigns.portofino.model.database.Table
import com.manydesigns.portofino.modules.DatabaseModule
import com.manydesigns.portofino.persistence.Persistence
import com.manydesigns.portofino.reflection.TableAccessor
import com.manydesigns.portofino.shiro.AbstractPortofinoRealm
import com.manydesigns.portofino.shiro.PasswordResetToken
import org.apache.commons.lang.StringUtils
import org.apache.shiro.crypto.hash.Sha1Hash
import org.hibernate.Criteria
import org.hibernate.Session
import org.hibernate.criterion.Restrictions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.shiro.authc.*

class Security extends AbstractPortofinoRealm {

    public static final String ADMIN_GROUP_NAME = "admin";
    public static final String PROJECT_MANAGER_GROUP_NAME = "project-manager";

    private static final Logger logger = LoggerFactory.getLogger(Security.class);

    @Inject(DatabaseModule.PERSISTENCE)
    Persistence persistence;

    //--------------------------------------------------------------------------
    // Authentication
    //--------------------------------------------------------------------------

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        return loadAuthenticationInfo(token);
    }

// 
//    protected AuthenticationInfo loadAuthenticationInfo(ServletContainerToken token) {
//        def info = super.doGetAuthenticationInfo(token);
//        User user = (User) info.principals.asList()[0];
//
//        Session session = persistence.getSession("tt");
//        Criteria criteria = session.createCriteria("users");
//        criteria.add(Restrictions.eq("email", user.email));
//
//        Serializable principal = (Serializable)criteria.uniqueResult();
//
//        if (principal == null) {
//            throw new UnknownAccountException("Unknown user");
//        } else if (!principal.validated) {
//            throw new DisabledAccountException("User not validated");
//        } else {
//            logger.debug("Aggiorno i campi accesso.");
//            updateAccess(principal, new Date());
//            session.update("users", (Object)principal);
//            session.getTransaction().commit();
//        }
//
//        /*def pc = new SimplePrincipalCollection([principal, user], getName());
//        SimpleAuthenticationInfo infoEx =
//                new SimpleAuthenticationInfo(pc, "", getName());
//        return infoEx;*/
//        return new SimpleAuthenticationInfo(principal, "", getName());
//    }

    public AuthenticationInfo loadAuthenticationInfo(UsernamePasswordToken usernamePasswordToken) {
        String login = usernamePasswordToken.username;
        String plainTextPassword;
        if (usernamePasswordToken.password == null) {
            plainTextPassword = "";
        } else {
            plainTextPassword = new String(usernamePasswordToken.password);
        }

        String encryptedPassword = encryptPassword(plainTextPassword);
        Session session = persistence.getSession("tt");

        Criteria criteria = session.createCriteria("users");
        criteria.add(Restrictions.eq("email", login));

        Serializable principal = (Serializable)criteria.uniqueResult();

        if (principal == null) {
            throw new UnknownAccountException("Unknown user");
        } else if (!encryptedPassword.equals(principal.password)) {
            throw new IncorrectCredentialsException("Wrong password");
        } else if (principal.validated == null) {
            throw new DisabledAccountException("User not validated");
        } else {
            logger.debug("Aggiorno i campi accesso.");
            updateAccess(principal, new Date());
            session.update("users", (Object)principal);
            session.getTransaction().commit();
        }

        SimpleAuthenticationInfo info =
                new SimpleAuthenticationInfo(
                        principal, plainTextPassword.toCharArray(), getName());
        return info;
    }

    public AuthenticationInfo loadAuthenticationInfo(
            PasswordResetToken passwordResetToken) {
        String token = passwordResetToken.token;
        String newPassword = passwordResetToken.newPassword;
        if (!checkPasswordStrength(newPassword)) {
            SessionMessages.addErrorMessage("The password is not strong enough");
            throw new Exception();
        }

        String encryptedPassword = encryptPassword(newPassword);
        Session session = persistence.getSession("tt");
        Criteria criteria = session.createCriteria("users");
        criteria.add(Restrictions.eq("token", token));

        Serializable principal = (Serializable)criteria.uniqueResult();

        if (principal == null) {
            throw new IncorrectCredentialsException();
        } else {
            logger.debug("Updating fields.");
            updateAccess(principal, new Date());
            principal.token = null;
            if (principal.validated == null) {
                principal.validated = new Date();
            }
            principal.password = encryptedPassword;
            session.update("users", (Object)principal);
            session.getTransaction().commit();
        }

        SimpleAuthenticationInfo info =
                new SimpleAuthenticationInfo(
                        principal, token, getName());
        return info;
    }

    boolean checkPasswordStrength(String password) {
        return StringUtils.length(password) >= 8;
    }


    private void updateAccess(Object principal, Date now) {
        def request = ElementsThreadLocals.getHttpServletRequest();
        principal.last_access = now;
        principal.last_access_ip = request.getRemoteAddr();
    }


    public String encryptPassword(String plainText) {
        Sha1Hash sha1Hash = new Sha1Hash(plainText);
        String encrypted = sha1Hash.toHex();
        return encrypted;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof PasswordResetToken ||
               token instanceof UsernamePasswordToken;
    }

    //--------------------------------------------------------------------------
    // Authorization
    //--------------------------------------------------------------------------

    @Override
    protected Collection<String> loadAuthorizationInfo(Serializable principal) {
        List<String> result = new ArrayList<String>()
        if (principal.admin) {
            result.add(ADMIN_GROUP_NAME);
//            UserService userService = UserServiceFactory.getUserService();
//            if (userService.isUserAdmin()) {
//            if (isLocalUser()) {
//                result.add(SecurityLogic.getAdministratorsGroup(portofinoConfiguration));
//            }
        }
        if (principal.project_manager) {
            result.add(PROJECT_MANAGER_GROUP_NAME);
        }
        return result;
    }

    protected boolean isLocalUser() {
        String remoteIp =
            ElementsThreadLocals.getHttpServletRequest().getRemoteAddr();
        InetAddress clientAddr = InetAddress.getByName(remoteIp);
        return SendMailAction.isLocalIPAddress(clientAddr)
    }


    //--------------------------------------------------------------------------
    // Users crud
    //--------------------------------------------------------------------------

    @Override
    Map<Serializable, String> getUsers() {
        Session session = persistence.getSession("tt");
        Criteria criteria = session.createCriteria("users");
        List users = criteria.list();

        Map result = new HashMap();
        for (Serializable user : users) {
            long id = user.id;
            String prettyName = getUserPrettyName(user);
            result.put(id, prettyName);
        }

        return result;
    }

    @Override
    Set<String> getGroups() {
        Set<String> result = super.getGroups();
        result.add(ADMIN_GROUP_NAME);
        result.add(PROJECT_MANAGER_GROUP_NAME);
        return result;
    }



    @Override
    Serializable getUserById(String encodedUserId) {
        Session session = persistence.getSession("tt");
        return (Serializable) session.get("users", Long.parseLong(encodedUserId));
    }

    Serializable getUserByEmail(String email) {
        Session session = persistence.getSession("tt");
        def criteria = session.createCriteria("users");
        criteria.add(Restrictions.eq("email", email));
        return (Serializable) criteria.uniqueResult();
    }

    @Override
    String getUserPrettyName(Serializable user) {
        return "${user.first_name} ${user.last_name}";
    }

    Serializable getUserId(Serializable user) {
        return user.id
    }

    @Override
    Serializable saveUser(Serializable user) {
        def session = persistence.getSession("tt");
        session.save("users", (Object) user);
        session.transaction.commit();
        return user;
    }

    @Override
    Serializable updateUser(Serializable user) {
        def session = persistence.getSession("tt");
        session.update("users", (Object) user);
        session.transaction.commit();
        return user;
    }

    @Override
    ClassAccessor getUserClassAccessor() {
        Database database =
            DatabaseLogic.findDatabaseByName(persistence.model, "tt");
        Table table =
            DatabaseLogic.findTableByEntityName(database, "users");
        return new TableAccessor(table);
    }

    @Override
    void changePassword(Serializable user, String oldPassword, String newPassword) {
        String encryptedOldPassword = encryptPassword(oldPassword);
        String encryptedNewPassword = encryptPassword(newPassword);
        def session = persistence.getSession("tt");
        def q = session.createQuery(
                "update users set password = :newPassword where id = :id and password = :oldPassword");
        q.setParameter("newPassword", encryptedNewPassword);
        q.setParameter("oldPassword", encryptedOldPassword);
        q.setParameter("id", user.id);
        if(q.executeUpdate() != 1) {
            throw new RuntimeException("Password not changed");
        }
        session.getTransaction().commit();
    }

    @Override
    String generateOneTimeToken(Serializable principal) {
        Session session = persistence.getSession("tt");
        String token = RandomUtil.createRandomId();
        principal.token = token;
        session.save("users", (Object)principal);
        session.getTransaction().commit();

        return token;
    }


}