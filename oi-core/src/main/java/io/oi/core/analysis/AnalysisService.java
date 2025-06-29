package io.oi.core.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.DoStmt;
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
    private static final Map<String, List<String>> conditionalBranchesCache = new ConcurrentHashMap<>();
    private static final JavaParser javaParser = new JavaParser();

    public static Map<String, Object> getParameterMap(Method method, Object[] args) {
        if (method == null || args == null) {
            return Map.of();
        }
        
        Map<String, Object> paramMap = new LinkedHashMap<>();
        String[] paramNames = getParameterNames(method);
        
        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            paramMap.put(paramNames[i], args[i]);
        }
        return paramMap;
    }

    public static List<String> getConditionalBranches(String className, String methodName, String methodDescriptor) {
        String cacheKey = className + "#" + methodName + "#" + methodDescriptor;
        
        return conditionalBranchesCache.computeIfAbsent(cacheKey, k -> {
            try {
                return analyzeConditionalBranches(className, methodName, methodDescriptor);
            } catch (Exception e) {
                log.debug("Failed to analyze conditional branches for {}.{}: {}", className, methodName, e.getMessage());
                return List.of();
            }
        });
    }

    private static List<String> analyzeConditionalBranches(String className, String methodName, String methodDescriptor) {
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
                
                // Find the method by name (simplified - in practice you'd match by signature)
                Optional<MethodDeclaration> optMethod = cu.findFirst(MethodDeclaration.class, 
                    md -> md.getNameAsString().equals(methodName));
                
                if (optMethod.isPresent()) {
                    MethodDeclaration md = optMethod.get();
                    List<String> branches = new ArrayList<>();
                    
                    // Find if statements
                    md.findAll(IfStmt.class).forEach(stmt -> {
                        String condition = stmt.getCondition().toString();
                        branches.add("if(" + condition + ")");
                    });
                    
                    // Find switch statements
                    md.findAll(SwitchStmt.class).forEach(stmt -> {
                        String selector = stmt.getSelector().toString();
                        branches.add("switch(" + selector + ")");
                    });
                    
                    // Find loops (for, while, do-while)
                    md.findAll(ForStmt.class).forEach(stmt -> {
                        String condition = stmt.getCompare().map(c -> c.toString()).orElse("true");
                        branches.add("for(" + condition + ")");
                    });
                    
                    md.findAll(WhileStmt.class).forEach(stmt -> {
                        String condition = stmt.getCondition().toString();
                        branches.add("while(" + condition + ")");
                    });
                    
                    md.findAll(DoStmt.class).forEach(stmt -> {
                        String condition = stmt.getCondition().toString();
                        branches.add("do-while(" + condition + ")");
                    });
                    
                    return branches;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse source for {}: {}", className, e.getMessage());
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
                    // Fallback to arg0, arg1, etc. if parameter names are not available
                    names[i] = "arg" + i;
                }
            }
            return names;
        });
    }

    /**
     * Clears the internal caches. Useful for testing or when memory usage becomes a concern.
     */
    public static void clearCaches() {
        parameterNamesCache.clear();
        conditionalBranchesCache.clear();
    }
} 