package timp.service;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "mail.imap")
@Service
public class EmailReceiverService {
    private final FireAccessService fireAccessService;
    @Value("${spring.mail.username}") 
    private String username;
    @Value("${spring.mail.password}") 
    private String password;
    @Value("${mail.imap.host}") 
    private String host;
    @Value("${mail.imap.port}") 
    private int port;
    @Value("${mail.imap.ssl.enable}") 
    private String sslEnable;
    @Value("${mail.imap.auth}") 
    private String auth;
    
    public EmailReceiverService(FireAccessService fireAccessService) {
        this.fireAccessService = fireAccessService;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkEmails() {
        try {
            Properties props = new Properties();
            props.put("mail.imap.ssl.enable", sslEnable);
            props.put("mail.imap.auth", auth);

            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(host, port, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message msg : messages) {
                String subject = msg.getSubject().toLowerCase().trim();
                String content = getTextFromMessage(msg).trim();
                Long id;
                try {
                    id = Long.parseLong(content);
                } catch(Exception e) {
                    continue;
                }
                if(subject.equals("открыть")) {
                    fireAccessService.setOpen(id, true);
                } else if(subject.equals("закрыть")) {
                    fireAccessService.setOpen(id, false);
                }
            }
            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("IMAP check failed: " + e.getMessage());
        }
    }

    private String getTextFromMessage(Message msg) throws Exception {
        if (msg.isMimeType("text/plain")) {
            return (String) msg.getContent();
        }
        if (msg.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) msg.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart part = mp.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    return part.getContent().toString();
                }
            }
        }
        return "";
    }
}
