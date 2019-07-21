package net.dirtcraft.dirtlauncher.backend.utils;

import net.cydhra.nidhogg.MojangClient;
import net.cydhra.nidhogg.YggdrasilAgent;
import net.cydhra.nidhogg.YggdrasilClient;
import net.cydhra.nidhogg.data.AccountCredentials;
import net.cydhra.nidhogg.data.NameEntry;
import net.cydhra.nidhogg.data.Session;
import net.cydhra.nidhogg.exception.InvalidCredentialsException;
import net.dirtcraft.dirtlauncher.backend.config.Constants;
import net.dirtcraft.dirtlauncher.backend.objects.Account;

import java.util.List;
import java.util.UUID;

public class Verification {

    public static Account login(String email, String password) throws InvalidCredentialsException {

        YggdrasilClient client = new YggdrasilClient();

        Session session = client.login(new AccountCredentials(email, password), YggdrasilAgent.MINECRAFT);
        final UUID uuid = session.getUuid();

        MojangClient mojangClient = new MojangClient(session.getClientToken());

        List<NameEntry> names = mojangClient.getNameHistoryByUUID(uuid);

        Account account = new Account(session, mojangClient, names.get(names.size() - 1).getName(), password, uuid, client.validate(session));

        if (Constants.VERBOSE) {
            System.out.println("USERNAME: " + account.getUsername());
            System.out.println("PASSWORD: " + account.getPassword());
            System.out.println("UUID: " + account.getUuid());
            System.out.println("IS AUTHENTICATED: " + account.isAuthenticated());
        }
        return account;

    }

}
