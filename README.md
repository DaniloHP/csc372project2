# Judo: Java Pseudocode

Judo is a simple, readable language that targets Java 1.8. It's strongly typed and determines types through type inference.
```text
for i in 1..100:
    if i mod 15 == 0:
        out("fizzbuzz")
    elf i mod 5 == 0:
        out("buzz")
    elf i mod 3 == 0:
        out("fizz")
```
The Translator also only requires Java 1.8 to run. Due to the scope of the project, a precompiled JAR file can be found in our [latest release](https://github.com/DaniloHP/csc372project2/releases/latest) and in this repository at [`/jar/judot.jar`](https://github.com/DaniloHP/csc372project2/blob/main/jar/judot.jar).

## Translating and running a Judo file
Before running, make sure you have at least Java 1.8 by running:
```shell
$ java -version
```
The output will vary depending on your JDK implementation, but we want to see `1.8.*` somewhere in there.

To run the Judo translator, execute the JAR we provided like so:
```shell
$ java -jar path/to/jar path/to/judo/file  
# judot will tell you where it saved the translated java file.
# Run it like a normal Java file:
$ javac path/to/java/file.java  # don't forget .java here
$ java path/to/java/file  # no .java here
```
Or, we provided a [convenience script](https://github.com/DaniloHP/csc372project2/tree/main/judot) which is used like so:
```shell
$ ./judot path/to/jar path/to/judo/file judo file arguments
```
For example, to run our program 4 from the root of the repository:
```shell
$ ./judot jar/judot.jar judo-files/required/Program4.txt 10

   *
  ***
 *****
*******
 *****
  ***
   *
```
Our program 4 creates a star triangle. If the Judo file translates correctly, you should just see the Java file's output like above.
# For Graders
Our programs 1-5 are in [`/judo-files/required/`](https://github.com/DaniloHP/csc372project2/tree/main/judo-files/required).

I know the expression validation logic might be a little obscure, so this is meant to be a visual explanation of how it works, using the `toCheck = "3 + x * 2"`:

```java
public boolean validate(CharSequence toCheck) {
    for (Rule r : levels.get(0)) {
        if (r.validate(toCheck)) {
            return true;
        }
    }
    return false;
}
```
1. Call `MathGrammar::validate`, shown above, on the expression. This will iterate through the first element in `levels`, where levels is an ArrayList looks like this:
```
[
    0: [Rule ADDITION, Rule SUBTRACTION, Rule ADDITION_RIGHT, Rule SUBTRACTION_RIGHT, Rule DOWN_AS]
    
    1: [Rule MULTIPLICATION, Rule DIVISION, Rule MODULUS, Rule MULTIPLICATION_RIGHT, Rule DIVISION_RIGHT, Rule MODULUS_RIGHT, Rule DOWN_MMD]
    
    2: [VarRule MATH_VAR (int), Rule INTEGERS, Rule PARENTHESES, Rule UNARY_NEGATIVE]
]
```
Visually, this looks exactly like the way we write out grammars on paper. One thing you'll notice is that all binary rules have two versions, one of which has an ID ending with `_RIGHT`. These versions have a lazy quantifier `.*?` on the left part of the expression. Without these rules, an expression like `3 + (4 + 5)` doesn't appear valid because it'll be split into the groups `3 + (4` and `5)`. Clearly, the expression actually is valid, and a lazy quantifier on the left side will give us the groups `3` and `(4 + 5)`, which will evaluate as it should. 

Also, each level of a grammar usually has a rule with and ID ending in `_DOWN`. This is a rule with a simple `(.*)` regular expression that accepts anything, and serves to move expression down the tree. It is equivalent to how Backus–Naur grammars tend to include a rule like ` ... | <mul-expr> | ....` that doesn't have any requirements. Moving on...
2. So we're iterating through the fist element in `levels`, which is element 0 in the object above. `levels[0][0]`, which is `Rule ADDITION`, will be the first Rule which we call `validate("3 + x * 2")` on. Here is `Rule::validate`:
```java
public boolean validate(CharSequence toCheck) {
    Matcher matcher = regex.matcher(toCheck);
    if (toCheck.length() > 0 && matcher.matches()) {
        if (this.isTerminal()) {
            return true; //(1)
        }
        boolean[] resultVector = new boolean[children.size()];
        int i = 0;
        for (String groupName : children.keySet()) {
            String currGroup = matcher.group(groupName).trim();
            for (Rule rule : children.get(groupName)) {
                if (rule.validate(currGroup)) {
                    resultVector[i] = true; //(2)
                    if (allTrue(resultVector)) {
                        return true; //(3)
                    }
                    break;
                }
            }
            i++;
        }
    }
    return false;
}
```
And here is `Rule ADDITION`'s regular expression:
```regexp
(?<left>.*)\+(?<right>.*)
```
Two named capture-anything groups, one called `left` and the other called `right`, both with greedy `*` quantifiers, separated by a `+`. Clearly, `toCheck` is not empty, and the regular expression will match and split up `toCheck` into the groups `left="3 "` and `right=" x * 2"`. Now, we'll iterate through the `this.children`, which contains `{"left" -> 0, "right" -> 1}` where the numbers represent indexes from the array diagram above, under 1.

3. Whether the first `groupName` is `"left"` or `"right"` isn't necessarily known since `children` is a HashMap, but let's assume it's left. `currGroup = "3"`, and now we will iterate over each Rule in which is a child of the `left` group. Since our grammar is set up to be left associative, that would be a recursive call in Backus–Naur form. This means that we'll try to evaluate `"3"` under each of the Rules in `[Rule ADDITION, Rule SUBTRACTION, Rule ADDITION_RIGHT, Rule SUBTRACTION_RIGHT, Rule DOWN_AS]`, and it'll take down rules until it reaches `Rule INTEGER` on level 3, where it will naturally pass the regex `\d+`. Since `Rule INTEGER` is terminal, it will simply return true at `(1)`, and the recursion will unwind until we reach the original `Rule ADDITION` and `true` will be put into `resultVector[0]` at `(2)`. `resultVector[1]` is still false, so we won't return true yet, but we will break and move onto the next group.
4. Next, we'll evaluate the `"right"` group, which is `"x * 2"`. This group's children are `[Rule MULTIPLICATION, Rule DIVISION, Rule MODULUS, Rule MULTIPLICATION_RIGHT, Rule DIVISION_RIGHT, Rule MODULUS_RIGHT, Rule DOWN_MMD]`, of which `Rule MULTIPLICATION` will be the first to validate. From there, it'll be split up into `"x"` and `"2"`.
5. `"2"` will eventually trivially evaluate to `true` as an integer. `"x"` is a little more interesting though. It will make it's way down to the `VarRule MATH_VAR (int)`, which will call the following function due to dynamic dispatch.
```java
public boolean validate(CharSequence toCheck, boolean doKWCheck/* = true */, boolean doTypeCheck/* = true */) {
    Matcher m = this.regex.matcher(toCheck);
    if (toCheck.length() > 0 && m.matches()) {
        String varName = m.group("var");
        if (doKWCheck && VarRule.NONVALUE_KEYWORDS.contains(varName)) { //(1)
            throw new VariableError(
                format("Variable `{0}` uses a reserved keyword for its name", varName)
            );
        }
        if (VarRule.scopes != null/* = true */) {
            Variable var = BUILTINS_AS_VARIABLES.get(varName); //(2)
            var = var == null ? scopes.find(varName, true) : var;
            if (doTypeCheck && this.expectedType != null && var.type != this.expectedType) { //(3)
                throw new TypeError(
                    format(
                        "Variable `{0}` was expected to be of type {1}",
                        varName,
                        this.expectedType.javaType
                    )
                );
            }
        }
        return true;
    }
    return false;
}
```
Assume `doKwCheck` and `doTypeCheck` are both true and `VarRule.scopes` is non-null, which they would be in a non-testing scenario. `"x"` easily passes the variable regex, and `varName` is just `toCheck` with no leading or trailing whitespace. `"x"` isn't a keyword, so it passes `(1)`. `"x"` isn't a value keyword either, so `var` will be null at `(2)`. On the next line, it will be looked up in `scopes`, which will look for a variable with the name `x` in current and past scopes: 
```java
public Variable find(String varName, boolean doThrow/* = true */) {
    String trimmed = varName.trim();
    for (int i = this.size() - 1; i >= 0; i--) {
        Map<String, Variable> scope = this.get(i);
        if (scope.containsKey(trimmed)) {
            return scope.get(trimmed);
        }
    }
    if (doThrow) {
        throw new VariableError(format("Variable `{0}` not found", trimmed));
    }
    return null;
}
```
Assuming `x` is previously defined, it's `Variable` object will be returned. Now, at `(3)` in the `validate()` code block, we will expect the type of `x` to be `Type.INT`, since the math grammar's variable rule is set to expect integers (recall that it's a `VarRule MATH_VAR (int)`, meaning it's expecting ints). Assuming `x` was previously defined as an int, this `validate` will also return true.

6. Back in our very first validate, `Rule ADDITION`, both children will have returned true, and `allTrue(resultVector)` will be true, thus returning true and taking us *all* the way back to `Grammar::validate`. This function immediately returns  true once a Rule validates, meaning that our expression `3 + x * 2` is fully validated.