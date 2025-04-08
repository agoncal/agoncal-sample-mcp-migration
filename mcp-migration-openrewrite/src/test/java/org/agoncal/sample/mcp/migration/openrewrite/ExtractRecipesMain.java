package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openrewrite.Contributor;
import org.openrewrite.Recipe;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeExample;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.java.migrate.UpgradeJavaVersion;

public class ExtractRecipesMain {


    public static void main(String[] args) throws JsonProcessingException {

        // Display info on Recipes
        displayInfoRecipe(UpgradeJavaVersion.class);

        // Displays all the recipes in JSON format
        recipeToJson();

        // Generate a Tool
        generateTool();
    }

    private static void generateTool() {

    }

    private static void displayInfoRecipe(Class recipeClass) {
        Recipe recipe = RecipeIntrospectionUtils.constructRecipe(recipeClass);
        System.out.println("\n-- Recipe ---");
        System.out.println("\trecipe.getName()\t\t: " + recipe.getName());
        System.out.println("\trecipe.getSimpleName()\t: " + recipe.getClass().getSimpleName());
        System.out.println("\trecipe.getDisplayName()\t: " + recipe.getDisplayName());
        System.out.println("\trecipe.getDescription()\t: " + recipe.getDescription());
        System.out.println("\trecipe.getInstanceName(): " + recipe.getInstanceName());
        System.out.println("\trecipe.getInstanceNameSuffix()\t: " + recipe.getInstanceNameSuffix());
        System.out.println("\trecipe.getClass().getName()\t: " + recipe.getClass().getName());
        System.out.println("\trecipe.getJacksonPolymorphicTypeTag()\t: " + recipe.getJacksonPolymorphicTypeTag());
        System.out.println("\trecipe.getEstimatedEffortPerOccurrence()\t: " + recipe.getEstimatedEffortPerOccurrence());

        System.out.println("\n-- Descriptor ---");
        System.out.println("\trecipe.getDescriptor().getName()\t: " + recipe.getDescriptor().getName());
        System.out.println("\trecipe.getDescriptor().getDescription()\t: " + recipe.getDescriptor().getDescription());
        System.out.println("\trecipe.getDescriptor().getDisplayName()\t: " + recipe.getDescriptor().getDisplayName());
        System.out.println("\trecipe.getDescriptor().getInstanceName()\t: " + recipe.getDescriptor().getInstanceName());

        System.out.println("\n-- Descriptor Options ---");
        for (OptionDescriptor optionDescriptor : recipe.getDescriptor().getOptions()) {
            System.out.println("\tgetName()\t\t:" + optionDescriptor.getName());
            System.out.println("\tgetDisplayName():" + optionDescriptor.getDisplayName());
            System.out.println("\tgetDescription():" + optionDescriptor.getDescription());
            System.out.println("\tgetType()\t\t:" + optionDescriptor.getType());
            System.out.println("\tgetValue()\t\t:" + optionDescriptor.getValue());
            System.out.println("\tgetExample()\t:" + optionDescriptor.getExample());
        }

        System.out.println("\n-- Example ---");
        for (RecipeExample example : recipe.getExamples()) {
            System.out.println("\tgetName()\t:" + example.getDescription());
            System.out.println("\tgetEmail()\t:" + example.getParameters());
            System.out.println("\tgetLineCount()\t:" + example.getSources().size());
        }

        System.out.println("\n-- Recipe List ---");
        for (Recipe recipeEmbedded : recipe.getRecipeList()) {
            System.out.println("\tgetEmail()\t:" + recipeEmbedded.getName());
            System.out.println("\tgetName()\t:" + recipeEmbedded.getDescription());
            System.out.println("\tgetLineCount()\t:" + recipeEmbedded.getDisplayName());
        }

        System.out.println("\n-- Contributors ---");
        for (Contributor contributor : recipe.getContributors()) {
            System.out.println("\tgetName()\t:" + contributor.getName());
            System.out.println("\tgetEmail()\t:" + contributor.getEmail());
            System.out.println("\tgetLineCount()\t:" + contributor.getLineCount());
        }

        System.out.println("\n-- Tags ---");
        for (String tag : recipe.getTags()) {
            System.out.println("\t" + tag);
        }

        System.out.println("\n-- DataTableDescriptors ---");
        for (DataTableDescriptor dataTableDescriptor : recipe.getDataTableDescriptors()) {
            System.out.println("\tgetName()\t\t:" + dataTableDescriptor.getName());
            System.out.println("\tgetDisplayName()\t:" + dataTableDescriptor.getDisplayName());
            System.out.println("\tgetDescription()\t:" + dataTableDescriptor.getDescription());
        }
    }

    private static void recipeToJson() throws JsonProcessingException {
        System.out.println(new MigrationOpenRewriteMCPServer().getRecipeAsJson());
    }
}
