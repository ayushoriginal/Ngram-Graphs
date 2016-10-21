/*
 * Notifier.java
 *
 * Created on 18 Οκτώβριος 2006, 6:48 μμ
 *
 */

package gr.demokritos.iit.jinsect.events;

/**
 *
 * @author ggianna
 */
public interface Notifier {
    public void setNotificationListener(NotificationListener nlListener);
    public void removeNotificationListener();
    public NotificationListener getNotificationListener();
}
