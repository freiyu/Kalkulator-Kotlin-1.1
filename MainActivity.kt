package com.example.kotlincalculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
  private lateinit var workings: TextView
  private lateinit var result: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    workings = findViewById(R.id.workings)
    result = findViewById(R.id.result)

    setupNumberButtons()
    setupOperatorButtons()
    setupEqualsButton()
    setupClearButton()
    setupTrigButtons()
    setupBackspaceButton()
    setupPercentageButton()
    setupCombinationPermutationButtons()
    setupConversionButtons()
  }

  private var isAdvancedMode = false
  private var currentExpression = mutableListOf<String>()
  private var isResultShown = false

  fun toggleAdvancedMode(view: View) {
    val advLayout = findViewById<LinearLayout>(R.id.advancedButtonLayout)
    val advancedButton = findViewById<Button>(R.id.advancedToggle)

    isAdvancedMode = !isAdvancedMode

    if (isAdvancedMode) {
      advLayout.visibility = View.VISIBLE
      advancedButton.text = "<<"
    }
    else {
      advLayout.visibility = View.GONE
      advancedButton.text = ">>"
    }
  }

  private fun setupNumberButtons() {
    val numberButtonIds = listOf(
      R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
      R.id.btn4, R.id.btn5, R.id.btn6,
      R.id.btn7, R.id.btn8, R.id.btn9,
      R.id.btnDecimal
    )

    numberButtonIds.forEach { buttonId ->
      findViewById<Button>(buttonId).setOnClickListener {
        addNumber((it as Button).text.toString())
      }
    }
  }

  //trigonometry utility
  private fun setupTrigButtons() {
    val trigButtons = listOf(
      R.id.sinButton to "sin",
      R.id.cosButton to "cos",
      R.id.tanButton to "tan",
      R.id.logButton to "log"
    )

    trigButtons.forEach { (buttonId, operation) ->
      findViewById<Button>(buttonId).setOnClickListener {
        addTrigFunction(operation)
      }
    }
  }

  private fun addTrigFunction(function: String) {
    if (isResultShown) {
      currentExpression.clear()
      isResultShown = false
    }

    if (currentExpression.isNotEmpty()) {
      val lastEntry = currentExpression.last()
      if (lastEntry.matches(Regex("[0-9.]")) || lastEntry == ")") {
        currentExpression.add("x")
      }
    }

    currentExpression.add(function)
    currentExpression.add("(")

    updateDisplay()
  }

  //numbers utility
  private fun addNumber(number: String) {
    if (number == "." &&
      (currentExpression.isEmpty() ||
          currentExpression.last().contains("."))) return

    currentExpression.add(number)
    updateDisplay()
  }

  private fun setupOperatorButtons() {
    val operatorButtonIds = listOf(
      R.id.btnPlus, R.id.btnMinus,
      R.id.btnMultiply, R.id.btnDivide
    )

    operatorButtonIds.forEach { buttonId ->
      findViewById<Button>(buttonId).setOnClickListener {
        addOperator((it as Button).text.toString())
      }
    }
  }

  private fun addOperator(operator: String) {
    if (currentExpression.isNotEmpty() &&
      !isOperator(currentExpression.last()) &&
      currentExpression.last() != "(") {

      currentExpression.add(operator)
      updateDisplay()
    }
  }

  private fun setupEqualsButton() {
    findViewById<Button>(R.id.btnEquals).setOnClickListener {
      calculateResult()
    }
  }

  private fun calculateResult() {
    val workingsView = findViewById<TextView>(R.id.workings)
    val resultView = findViewById<TextView>(R.id.result)

    try {
      if (permutationMode || combinationMode) {
        handlePermutationCombinationEquals()
        return
      }

      while (currentExpression.count { it == "(" } >
        currentExpression.count { it == ")" }) {
        currentExpression.add(")")
      }

      val result = evaluateExpression(currentExpression)

      workingsView.text = currentExpression.joinToString("")
      resultView.text = result.toString()

      currentExpression = mutableListOf(result.toString())
      isResultShown = true

    }
    catch (e: Exception) {
      resultView.text = "Error"
    }
  }

  private fun updateDisplay() {
    val workingsView = findViewById<TextView>(R.id.workings)
    val displayExpression = buildDisplayExpression(currentExpression)

    workingsView.text = displayExpression
  }

  private fun buildDisplayExpression(expression: List<String>): String {
    val displayParts = mutableListOf<String>()
    var currentNumber = ""

    for (token in expression) {
      if (token.toDoubleOrNull() != null) {
        currentNumber += token
      }
      else {
        if (currentNumber.isNotEmpty()) {
          displayParts.add(currentNumber)
          currentNumber = ""
        }
        displayParts.add(token)
      }
    }

    if (currentNumber.isNotEmpty()) {
      displayParts.add(currentNumber)
    }

    return displayParts.joinToString("")
  }

  private fun setupClearButton() {
    findViewById<Button>(R.id.btnAC).setOnClickListener {
      resetCalculator()
    }
  }

  private fun resetCalculator() {
    val workingsView = findViewById<TextView>(R.id.workings)
    val resultView = findViewById<TextView>(R.id.result)

    currentExpression.clear()
    isResultShown = false

    workingsView.text = ""
    resultView.text = "0"
  }

  private fun isOperator(token: String): Boolean {
    return token in listOf("+", "-", "x", "/")
  }

  private fun evaluateExpression(expression: List<String>): Double {
    val processedExpression = processSpecialFunctions(expression)
    return MathUtils.evaluateExpression(processedExpression)
  }

  //processing functions
  private fun processSpecialFunctions(expression: List<String>): List<String> {
    val processed = mutableListOf<String>()
    var i = 0
    while (i < expression.size) {
      when (expression[i]) {
        "sin", "cos", "tan", "log", "P", "C" -> {
          // Find the closing parenthesis
          var closingIndex = i + 2
          var parenthesesCount = 1
          var functionValue = ""

          while (closingIndex < expression.size && parenthesesCount > 0) {
            if (expression[closingIndex] == "(") parenthesesCount++
            if (expression[closingIndex] == ")") parenthesesCount--

            if (parenthesesCount > 0) {
              functionValue += expression[closingIndex]
            }
            closingIndex++
          }

          val values = functionValue.split(",").map {
            try {
              evaluateExpression(it.trim().split(" ").filter { it.isNotBlank() })
            } catch (e: Exception) {
              it.trim().toDoubleOrNull() ?: 0.0
            }
          }

          // Perform the specific function calculation
          val result = when (expression[i]) {
            "sin" -> kotlin.math.sin(values[0])
            "cos" -> kotlin.math.cos(values[0])
            "tan" -> kotlin.math.tan(values[0])
            "log" -> kotlin.math.log10(values[0])
            "P" -> calculatePermutation(values[0].toInt(), values[1].toInt())
            "C" -> calculateCombination(values[0].toInt(), values[1].toInt())
            else -> 0.0
          }

          processed.add(result.toString())

          // Move index to the end of the function
          i = closingIndex
        }
        else -> {
          processed.add(expression[i])
        }
      }
      i++
    }
    return processed
  }

  private fun setupBackspaceButton() {
    findViewById<Button>(R.id.btnBackspace).setOnClickListener {
      performBackspace()
    }
  }

  private fun performBackspace() {
    val workingsView = findViewById<TextView>(R.id.workings)
    val resultView = findViewById<TextView>(R.id.result)

    if (currentExpression.isNotEmpty()) {
      currentExpression.removeAt(currentExpression.lastIndex)
      if (currentExpression.isEmpty()) {
        workingsView.text = ""
        resultView.text = "0"
      } else {
        updateDisplay()
      }
    }
  }

  //percentage utility
  private fun setupPercentageButton() {
    findViewById<Button>(R.id.btnPercent).setOnClickListener {
      addPercentage()
    }
  }

  private fun addPercentage() {
    if (isResultShown) {
      currentExpression.clear()
      isResultShown = false
    }
    if (currentExpression.isNotEmpty()) {
      try {
        val fullExpressionValue = evaluateExpression(currentExpression)
        val percentageValue = fullExpressionValue / 100.0

        currentExpression.clear()
        currentExpression.add(percentageValue.toString())
        updateDisplay()

      } catch (e: Exception) {
        result.text = "Error"
      }
    }
  }

  //helper
  private fun extractFunctionValue(expression: List<String>, index: Int): Double {
    if (index + 2 < expression.size) {
      var j = index + 2
      var value = ""

      // Collect all tokens until closing parenthesis
      while (j < expression.size && expression[j] != ")") {
        value += expression[j]
        j++
      }

      // Try to parse the value directly
      return value.toDoubleOrNull()
        ?: evaluateExpression(value.split(" ").filter { it.isNotBlank() })
    }
    throw IllegalArgumentException("Invalid function call")
  }

  //permutation and combination utility
  private fun factorial(n: Int): Double {
    return if (n == 0 || n == 1) 1.0
    else n * factorial(n - 1)
  }

  private fun calculatePermutation(n: Int, r: Int): Double {
    return if (n < r) 0.0
    else factorial(n) / factorial(n - r)
  }

  private fun calculateCombination(n: Int, r: Int): Double {
    return if (n < r) 0.0
    else factorial(n) / (factorial(r) * factorial(n - r))
  }

  //permutation and combination setup
  private var permutationMode = false
  private var combinationMode = false
  private var firstNumberEntered = false
  private fun setupCombinationPermutationButtons() {
    val permButton = findViewById<Button>(R.id.permutationButton)
    val combButton = findViewById<Button>(R.id.combinationButton)

    permButton.setOnClickListener {
      resetPermutationCombinationMode()
      permutationMode = true
      currentExpression.clear()
      currentExpression.add("P(")
      updateDisplay()
    }

    combButton.setOnClickListener {
      resetPermutationCombinationMode()
      combinationMode = true
      currentExpression.clear()
      currentExpression.add("C(")
      updateDisplay()
    }
  }

  private fun resetPermutationCombinationMode() {
    permutationMode = false
    combinationMode = false
    firstNumberEntered = false
  }

  private fun handlePermutationCombinationEquals() {
    // Remove the last "(" if it exists
    if (currentExpression.lastOrNull() == "(") {
      currentExpression.removeAt(currentExpression.lastIndex)
    }

    val expressionString = currentExpression.joinToString("")
    val numbers = expressionString
      .replace("P(", "")
      .replace("C(", "")
      .replace(")", "")
      .split(",")

    if (numbers.size == 1) {
      // First number input
      firstNumberEntered = true
      currentExpression.add(",")
      updateDisplay()
    }
    else if (numbers.size == 2) {
      // Second number input
      try {
        val n = numbers[0].toInt()
        val r = numbers[1].toInt()

        val result = if (permutationMode) {
          calculatePermutation(n, r)
        }
        else {
          calculateCombination(n, r)
        }

        // Clear and show result
        currentExpression.clear()
        currentExpression.add(result.toString())
        isResultShown = true
        updateDisplay()

        // Reset modes
        resetPermutationCombinationMode()
      }
      catch (e: Exception) {
        // Handle invalid input
        currentExpression.clear()
        currentExpression.add("Error")
        resetPermutationCombinationMode()
        updateDisplay()
      }
    }
  }

  //hex and bin utility
  private fun setupConversionButtons() {
    val decToHexButton = findViewById<Button>(R.id.hexButton)
    val decToBinaryButton = findViewById<Button>(R.id.binaryButton)

    decToHexButton.setOnClickListener {
      convertDecimalToHex()
    }

    decToBinaryButton.setOnClickListener {
      convertDecimalToBinary()
    }
  }

  private fun convertDecimalToHex() {
    try {
      // Ensure we have a valid number to convert
      val decimalValue = evaluateExpression(currentExpression)
      val hexValue = decimalToHexConversion(decimalValue)

      // Clear current expression and show result
      currentExpression.clear()
      result.text = hexValue
      isResultShown = true
      updateDisplay()
    } catch (e: Exception) {
      currentExpression.clear()
      result.text = "Error"
      isResultShown = true
      updateDisplay()
    }
  }

  private fun convertDecimalToBinary() {
    try {
      // Ensure we have a valid number to convert
      val decimalValue = evaluateExpression(currentExpression)
      val binaryValue = decimalToBinaryConversion(decimalValue)

      // Clear current expression and show result
      currentExpression.clear()
      result.text = binaryValue
      isResultShown = true
      updateDisplay()
    } catch (e: Exception) {
      currentExpression.clear()
      result.text = "Error"
      isResultShown = true
      updateDisplay()
    }
  }

  private fun decimalToHexConversion(decimal: Double): String {
    // Convert to integer first
    val intValue = decimal.toInt()

    // Handle negative numbers
    if (intValue < 0) {
      return "-" + Integer.toHexString(Math.abs(intValue)).uppercase()
    }

    return Integer.toHexString(intValue).uppercase()
  }

  private fun decimalToBinaryConversion(decimal: Double): String {
    // Convert to integer first
    val intValue = decimal.toInt()

    // Handle negative numbers
    if (intValue < 0) {
      return "-" + Integer.toBinaryString(Math.abs(intValue))
    }

    return Integer.toBinaryString(intValue)
  }
}