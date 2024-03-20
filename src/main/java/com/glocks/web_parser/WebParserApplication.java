package com.glocks.web_parser;

import com.glocks.web_parser.controller.MainController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class WebParserApplication {

	public static void main(String[] args) {

		ApplicationContext context =	SpringApplication.run(WebParserApplication.class, args);
		MainController mainController = (MainController) context.getBean("mainController");
		mainController.startProcess(context);

	}

}
