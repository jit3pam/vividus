/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.steps.ui.web;

import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.openqa.selenium.WebElement;
import org.vividus.context.VariableContext;
import org.vividus.softassert.ISoftAssert;
import org.vividus.steps.ComparisonRule;
import org.vividus.steps.ui.AbstractExecuteScriptSteps;
import org.vividus.steps.ui.validation.IBaseValidations;
import org.vividus.ui.action.search.Locator;
import org.vividus.ui.action.search.SearchParameters;
import org.vividus.ui.action.search.Visibility;
import org.vividus.ui.util.XpathLocatorUtils;
import org.vividus.ui.web.action.WebJavascriptActions;
import org.vividus.ui.web.action.search.WebLocatorType;
import org.vividus.variable.VariableScope;

public class CodeSteps extends AbstractExecuteScriptSteps
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IBaseValidations baseValidations;
    private final WebJavascriptActions javascriptActions;
    private final ISoftAssert softAssert;

    public CodeSteps(WebJavascriptActions javascriptActions, VariableContext variableContext, ISoftAssert softAssert,
            IBaseValidations baseValidations)
    {
        super(softAssert, variableContext);
        this.baseValidations = baseValidations;
        this.javascriptActions = javascriptActions;
        this.softAssert = softAssert;
    }

    /**
     * Checks that the <b>JSON</b> message returned after the <i>javascript call</i> contains the certain
     * <b>field</b> and checks the <b>value</b> of this field.
     * <p>
     * To make a JavaScript call one can open a <b><i>JavaScript console</i></b> in a browser <i>(via Ctrl+Shift+J)</i>,
     * type the <b>script</b> string and press <i>Enter</i>.
     * <p>
     * Actions performed at this step:
     * <ul>
     * <li>Removes the value of JSON message field;
     * <li>Checks the value with the specified one.
     * </ul>
     * @param script A string with the JavaScript command which returns a JSON message as a result
     * @param fieldName The name of the required field in a JSON message
     * @param value The required value of the field in a JSON message
     * @throws IOException If an input or output exception occurred
     * @see <a href="https://www.w3schools.com/json/"> <i>JSON format</i></a>
     */
    @Then("json generated by javascript `$script` contains field with name `$fieldName' and value `$value`")
    public void checkJsonFieldValue(String script, String fieldName, String value) throws IOException
    {
        JsonNode json = MAPPER.readTree(javascriptActions.executeScript("return JSON.stringify(" + script + ")")
                .toString());
        JsonNode actualFieldValue = json.get(fieldName);
        if (softAssert.assertNotNull(
                String.format("Field with the name '%s' was found in the JSON object", fieldName), actualFieldValue))
        {
            softAssert.assertEquals(
                    String.format("Field value from the JSON object is equal to '%s'", value),
                    value, actualFieldValue.textValue());
        }
    }

    /**
     * Steps checks the <a href="https://en.wikipedia.org/wiki/Favicon">favicon</a> of the tab
     * of the browser. It is displayed in DOM in the &lt;head&gt; tag.
     * <b> Example</b>
     * <pre>
     * &lt;head profile="http://www.w3.org/1999/xhtml/vocab"&gt;
     * &lt;link type="image/png" href="http://promo1dev/sites/promo1/files/<b>srcpart</b>" rel="shortcut icon" /&gt;
     * &lt;/head&gt;
     * </pre>
     * @param srcpart Part of URL with favicon
     */
    @Then("favicon with src containing `$srcpart` exists")
    public void ifFaviconWithSrcExists(String srcpart)
    {
        WebElement faviconElement = baseValidations.assertIfElementExists("Favicon",
                new Locator(WebLocatorType.XPATH,
                        new SearchParameters(XpathLocatorUtils.getXPath(
                                "//head/link[@rel='shortcut icon' or @rel='icon']"), Visibility.ALL)));
        if (faviconElement != null)
        {
            String href = faviconElement.getAttribute("href");
            if (softAssert.assertNotNull("Favicon contains 'href' attribute", href))
            {
                softAssert.assertThat("The favicon with the src containing " + srcpart + " exists", href,
                        containsString(srcpart));
            }
        }
    }

    /**
     * Checks that the <b>page code</b> contains a <b>node element</b> specified by <b>Locator</b>
     * @param locator        A locator for an expected element
     * @param comparisonRule The rule to match the quantity of elements. The supported rules:
     *                       <ul>
     *                       <li>less than (&lt;)</li>
     *                       <li>less than or equal to (&lt;=)</li>
     *                       <li>greater than (&gt;)</li>
     *                       <li>greater than or equal to (&gt;=)</li>
     *                       <li>equal to (=)</li>
     *                       <li>not equal to (!=)</li>
     *                       </ul>
     * @param quantity       The number to compare
     * @return <b>List&lt;WebElement&gt;</b> A collection of elements matching the locator,
     * <b> null</b> - if there are no desired elements
     */
    @Then("number of invisible elements `$locator` is $comparisonRule `$quantity`")
    public List<WebElement> doesInvisibleQuantityOfElementsExists(Locator locator,
            ComparisonRule comparisonRule, int quantity)
    {
        locator.getSearchParameters().setVisibility(Visibility.ALL);
        return baseValidations.assertIfNumberOfElementsFound("The number of found invisible elements",
                    locator, quantity, comparisonRule);
    }


    /**
     * Executes passed async javascript code on the opened page
     * and saves returned value into the <b>variable</b>
     * See {@link org.openqa.selenium.JavascriptExecutor#executeAsyncScript(String, Object[])}
     * <p>
     *
     * @param scopes       The set (comma separated list of scopes e.g.: STORY, NEXT_BATCHES) of variable's scope<br>
     *                     <i>Available scopes:</i>
     *                     <ul>
     *                     <li><b>STEP</b> - the variable will be available only within the step,
     *                     <li><b>SCENARIO</b> - the variable will be available only within the scenario,
     *                     <li><b>STORY</b> - the variable will be available within the whole story,
     *                     <li><b>NEXT_BATCHES</b> - the variable will be available starting from next batch
     *                     </ul>
     * @param variableName A name under which the value should be saved
     * @param jsCode       Code in javascript that returns some value as result
     *                     (e.g. var a=1; return a;)
     */
    @When("I execute async javascript `$jsCode` and save result to $scopes variable `$variableName`")
    public void saveValueFromAsyncJS(String jsCode, Set<VariableScope> scopes, String variableName)
    {
        assertAndSaveResult(() -> javascriptActions.executeAsyncScript(jsCode), scopes, variableName);
    }
}
