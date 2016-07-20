package ru.ogres.tools;

import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

/**
 * Created by zed on 20.07.16.
 */
@SpringUI(path = "/admin")
public class AdminUI extends UI {
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        setContent(new Button("Test"));
    }
}
