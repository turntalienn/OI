package io.oi.core.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final Map<Method, String[]> parameterNamesCache = new ConcurrentHashMap<>();
    private static final JavaParser javaParser = new JavaParser();

    public static Map<String, Object> getParameterMap(Method method, Object[] args) {
        Map<String, Object> paramMap = new LinkedHashMap<>();
        String[] paramNames = getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            paramMap.put(paramNames[i], args[i]);
        }
        return paramMap;
    }

    public static List<String> getConditionalBranches(String className, String methodName, String methodDescriptor) {
        String resourceName = className.replace('.', '/') + ".java";
        try (InputStream is = AnalysisService.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                log.trace("Could not find source file for class: {}", className);
                return List.of();
            }

            ParseResult<CompilationUnit> result = javaParser.parse(is);
            Optional<CompilationUnit> optCu = result.getResult();
            if (optCu.isPresent()) {
                CompilationUnit cu = optCu.get();
                // This is a simplified search. A real implementation would need to match method signatures.
                Optional<MethodDeclaration> optMethod = cu.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals(methodName));
                if (optMethod.isPresent()) {
                    MethodDeclaration md = optMethod.get();
                    List<String> branches = new ArrayList<>();
                    md.findAll(IfStmt.class).forEach(stmt -> branches.add("if(" + stmt.getCondition().toString() + ")"));
                    md.findAll(SwitchStmt.class).forEach(stmt -> branches.add("switch(" + stmt.getSelector().toString() + ")"));
                    return branches;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse source for {}", className, e);
        }
        return List.of();
    }


    private static String[] getParameterNames(Method method) {
        return parameterNamesCache.computeIfAbsent(method, m -> {
            Parameter[] parameters = m.getParameters();
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isNamePresent()) {
                    names[i] = parameters[i].getName();
                } else {
                    names[i] = "arg" + i;
                }
            }
            return names;
        });
    }
} 