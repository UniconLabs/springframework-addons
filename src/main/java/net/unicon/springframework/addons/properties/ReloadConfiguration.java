package net.unicon.springframework.addons.properties;

import java.util.ArrayList;
import java.util.List;

public class ReloadConfiguration implements Runnable {
    List<ReconfigurableBean> reconfigurableBeans;

    public void setReconfigurableBeans(final List reconfigurableBeans) {
        // early type check, and avoid aliassing
        this.reconfigurableBeans = new ArrayList<ReconfigurableBean>();
        for (final Object o : reconfigurableBeans) {
            this.reconfigurableBeans.add((ReconfigurableBean) o);
        }
    }

    public void run() {
        for (final ReconfigurableBean bean : reconfigurableBeans) {
            try {
                bean.reloadConfiguration();
            } catch (final Exception e) {
                throw new RuntimeException("while reloading configuration of " + bean, e);
            }
        }
    }
}
