package de.htwg.seapal.web.controllers;

import com.google.inject.Inject;
import com.typesafe.plugin.MailerAPI;
import com.typesafe.plugin.MailerPlugin;
import de.htwg.seapal.utils.logging.ILogger;
import de.htwg.seapal.web.controllers.helpers.Menus;
import de.htwg.seapal.web.controllers.helpers.PasswordHash;
import de.htwg.seapal.web.controllers.secure.IAccount;
import de.htwg.seapal.web.controllers.secure.IAccountController;
import de.htwg.seapal.web.controllers.secure.impl.Account;
import de.htwg.seapal.web.views.html.appContent.reset;
import de.htwg.seapal.web.views.html.appContent.signInSeapal;
import de.htwg.seapal.web.views.html.appContent.signUpSeapal;
import org.codehaus.jackson.node.ObjectNode;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;
import java.util.Random;

@With(Menus.class)
public class AccountAPI
        extends Controller {

    static Form<Account> form = Form.form(Account.class);

    @Inject
    private IAccountController controller;

    @Inject
    private ILogger logger;

    public Result signup() {
        Form<Account> filledForm = form.bindFromRequest();

        ObjectNode response = Json.newObject();
        IAccount account = filledForm.get();
        boolean exists = true;
        try {
            exists = controller.accountExists(account.getAccountName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (filledForm.hasErrors() || exists) {
            response.put("success", false);
            response.put("errors", filledForm.errorsAsJson());
            if (exists) {
                flash("errors", "Account already exists");
            }

            return badRequest(signUpSeapal.render(filledForm, routes.AccountAPI.signup()));
        } else {
            try {

                InputValidator.Error result = InputValidator.validate(account);
                if(result != InputValidator.Error.NONE) {
                    flash("errors", InputValidator.Error_Messages[result.ordinal()]);
                    return badRequest(signUpSeapal.render(filledForm, routes.AccountAPI.signup()));
                }

                account.setAccountPassword(PasswordHash.createHash(account.getAccountPassword()));
                controller.saveAccount(account);
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            session().clear();
            session(IAccountController.AUTHN_COOKIE_KEY, filledForm.get().getUUID().toString());
            return redirect(routes.Application.app());
        }
    }

    public Result login() {
        Form<Account> filledForm = DynamicForm.form(Account.class).bindFromRequest();


        ObjectNode response = Json.newObject();
        IAccount account = null;

        try {
            account = controller.authenticate(filledForm);

            if (!filledForm.hasErrors() && account != null) {
                session().clear();
                session(IAccountController.AUTHN_COOKIE_KEY, account.getUUID().toString());
                flash("success", "You've been logged in");
                return redirect(routes.Application.app());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.put("success", false);
        response.put("errors", filledForm.errorsAsJson());
        if (account == null) {
            flash("errors", "Wrong username or password");
        }

        return badRequest(signInSeapal.render(filledForm, routes.AccountAPI.login()));
    }

    public static Result logout() {
        session().clear();
        flash("success", "You've been logged out");
        return redirect(routes.Application.app());
    }

    private static final long TIMEOUT = 60 * 60 * 1000;

    public Result requestNewPassword() {
        Form<Account> filledForm = form.bindFromRequest();

        IAccount account = filledForm.get();
        List<? extends IAccount> list = controller.queryView("by_email", account.getAccountName());

        if (list.size() == 0) {
            return notFound("Account does not exist");
        } else if (list.size() > 1) {
            return internalServerError("Too many equal reset tokens");
        }

        account = list.get(0);

        Random rand = new Random(System.currentTimeMillis());

        int token = rand.nextInt(); // TODO: check if regex ^\s*-?[0-9]{1,10}\s*$ matches all possible return values of Random.nextInt()

        account.setResetToken(Integer.toString(token));

        account.setResetTimeout(System.currentTimeMillis() + TIMEOUT);

        controller.saveAccount(account);

        MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
        mail.setSubject("request for password change");
        logger.error("AccountName", account.getAccountName());
        mail.addRecipient("John Doe <" + account.getAccountName() + ">");
        mail.addFrom("seapalweb@gmail.com");
        mail.send("To Reset your password, click the following link: http://localhost:9000/pwreset/" + token);
        return ok();
    }

    public Result resetForm(int token) {
        return ok(reset.render(token));
    }

    public Result resetPassword() {
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        String token = form.get("token")[0];

        logger.error("Token", token);

        List<? extends IAccount> list = controller.queryView("resetToken", token);

        logger.error("size", Integer.toString(list.size()));

        if (list.size() == 0) {
            return notFound("Account does not exist");
        } else if (list.size() > 1) {
            return internalServerError("Too many equal reset tokens");
        }

        IAccount account = list.get(0);

        if (account.getResetTimeout() < System.currentTimeMillis()) {
            return forbidden("reset token expired");
        }

        account.setResetToken("0");

        account.setResetTimeout(0);

        try {
            account.setAccountPassword(PasswordHash.createHash(form.get("accountPassword")[0]));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        controller.saveAccount(account);

        return ok();
    }

    public static class Secured
            extends Security.Authenticator {

        @Override
        public String getUsername(Context ctx) {
            return ctx.session().get(IAccountController.AUTHN_COOKIE_KEY);
        }

        @Override
        public Result onUnauthorized(Context ctx) {
            return redirect(routes.Application.login());
        }
    }

    public static class SecuredAPI
            extends Security.Authenticator {

        @Override
        public String getUsername(Context ctx) {
            return ctx.session().get(IAccountController.AUTHN_COOKIE_KEY);
        }

        @Override
        public Result onUnauthorized(Context ctx) {
            ObjectNode response = Json.newObject();
            response.put("error","unauthorized");

            return unauthorized(response);
        }
    }
}