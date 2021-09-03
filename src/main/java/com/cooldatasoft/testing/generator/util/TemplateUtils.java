package com.cooldatasoft.testing.generator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class TemplateUtils {

    public static void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template) throws IOException {
        createFile(velocityEngine, context, outputFile, template, false);
    }

    public static void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template, boolean append) throws IOException {

        File file = new File(outputFile);
        if (!file.exists()) {
            file = file.getParentFile();
            Files.createDirectories(Paths.get(file.getAbsolutePath()));
        }


        Template t = velocityEngine.getTemplate(template);
        Writer writer = new FileWriter(outputFile, append);
        t.merge(context, writer);
        writer.flush();
        writer.close();
        log.info("Created : " + outputFile);
    }
}
