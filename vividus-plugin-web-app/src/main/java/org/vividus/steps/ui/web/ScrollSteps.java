/*
 * Copyright 2019-2024 the original author or authors.
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

import java.util.List;

import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.openqa.selenium.WebElement;
import org.vividus.annotation.Replacement;
import org.vividus.selenium.locator.Locator;
import org.vividus.softassert.ISoftAssert;
import org.vividus.steps.ui.validation.IBaseValidations;
import org.vividus.ui.context.IUiContext;
import org.vividus.ui.monitor.TakeScreenshotOnFailure;
import org.vividus.ui.web.action.DirectionScroller;
import org.vividus.ui.web.action.WebJavascriptActions;
import org.vividus.ui.web.action.WebUiContextScroller;

@TakeScreenshotOnFailure
public class ScrollSteps
{
    private final IUiContext uiContext;
    private final WebJavascriptActions javascriptActions;
    private final ISoftAssert softAssert;
    private final IBaseValidations baseValidations;

    public ScrollSteps(IUiContext uiContext, WebJavascriptActions javascriptActions, ISoftAssert softAssert,
            IBaseValidations baseValidations)
    {
        this.uiContext = uiContext;
        this.javascriptActions = javascriptActions;
        this.softAssert = softAssert;
        this.baseValidations = baseValidations;
    }

    /**
     * Scrolls current context to the picked direction;
     * If context is not set scroll will be applied to a page;
     * @param scrollDirection to apply scroll
     * <br>Possible directions:
     * <br><b>LEFT</b> - start of a page/element horizontally,
     * <br><b>RIGHT</b> - end of a page/element horizontally,
     * <br><b>TOP</b> - start of a page/element vertically,
     * <br><b>BOTTOM</b> - end of a page/element vertically
     */
    @When("I scroll context to $scrollDirection edge")
    public void scrollContextIn(ScrollDirection scrollDirection)
    {
        DirectionScroller scroller = new WebUiContextScroller(uiContext, javascriptActions);
        scrollDirection.scroll(scroller);
    }

    /**
     * Finds an element and scrolls it into view with centred positioning.
     * <br>
     * Please note that if the element to scroll is located inside an
     * <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/overflow">overflow</a> container then native JS
     * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView">scrollIntoView</a> method
     * with top alignment is used.
     *
     * @param locator The locator of the element to scroll into view
     */
    @When("I scroll element located by `$locator` into view")
    public void scrollIntoView(Locator locator)
    {
        List<WebElement> scrollTargets = baseValidations.assertIfElementsExist("Element to scroll into view", locator);
        if (!scrollTargets.isEmpty())
        {
            javascriptActions.scrollElementIntoViewportCenter(scrollTargets.get(0));
        }
    }

    /**
     * Checks if the page is scrolled to the specific element located by locator
     * <br>Example: &lt;a id="information_collection" name="information_collection_name"&gt; -
     * Then page is scrolled to element located by `id(information_collection)`
     * <p>
     * Actions performed at this step:
     * <ul>
     * <li>Assert that element with specified locator exists
     * <li>Checks whether page is scrolled to the very bottom
     * <li>If yes --&gt; verify that element's Y coordinate is positive which means that element is visible if no --&gt;
     * get element's Y coordinate and verify that it's close to 0 which means that element is an the very top
     * </ul>
     * @param locator A locator to locate element
     * @deprecated Use step: "Then element located by `$locator` $presence visible in viewport" instead
     */
    @Deprecated(since = "0.6.15", forRemoval = true)
    @Replacement(versionToRemoveStep = "0.8.0",
            replacementFormatPattern = "Then element located by `%1$s` is visible in viewport")
    @Then("page is scrolled to element located by `$locator`")
    public void isPageScrolledToElement(Locator locator)
    {
        WebElement element = baseValidations.assertIfElementExists("Element to verify position", locator);
        if (element != null)
        {
            int elementYCoordinate = element.getLocation().getY();
            boolean pageVisibleAreaScrolledToElement = javascriptActions.executeScript(String.format(
                    "var windowScrollY = Math.floor(window.scrollY);"
                    + "return windowScrollY <= %1$d && %1$d <= (windowScrollY + window.innerHeight)",
                    elementYCoordinate));
            softAssert.assertTrue(String.format("The page is scrolled to an element with located by %s", locator),
                    pageVisibleAreaScrolledToElement);
        }
    }
}
