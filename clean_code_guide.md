# Clean-Code-Guide für KI-gestützte Softwareentwicklung

metadata:
version: 1.0
format: deterministic-rule-set
target: humans-and-ai
language: de
strict_mode: true

---

## Grundprinzipien

- code_must_be_readable: true
- hidden_side_effects: forbidden
- one_responsibility_per_unit: required
- deterministic_behavior: required

---

## Größenlimits

rules:
class_max_lines: 100
method_max_lines: 60
recommended_method_max_lines: 30
max_nesting_depth: 3
max_method_parameters: 4
max_line_length: 120
max_direct_dependencies: 5

---

## Klassenregeln

class_rules:
- single_responsibility: required
- constructor_logic: minimal_only
- dependency_injection: required
- cyclic_dependencies: forbidden

allowed_constructor_logic:
- assignments
- validation
- lightweight_initialization

forbidden_constructor_logic:
- database_calls
- network_calls
- complex_calculations

---

## Methodenregeln

method_rules:
- one_task_only: required
- guard_clauses: preferred
- hidden_side_effects: forbidden
- boolean_control_parameters: forbidden

example_forbidden:
```java
save(user, true);
```

example_allowed:
```java
saveActiveUser(user);
```

---

## Naming-Konventionen

naming:
classes:
style: PascalCase
type: noun
forbidden:
- Utils
- Helper
- Manager

methods:
style: camelCase
type: verb_based

constants:
style: UPPER_SNAKE_CASE
immutable: required

variables:
abbreviations: forbidden
semantic_meaning: required

---

## Fehlerbehandlung

exceptions:
error_codes: forbidden
swallow_exceptions: forbidden
domain_specific_names: required

forbidden:
```java
catch (Exception e) {
}
```

required:
- logging
- rethrow
- domain_handling

---

## Kommentare

comments:
explain_what: forbidden
explain_why: required
outdated_comments: forbidden

todo_format:
```java
// TODO MaxMustermann: Retry-Mechanismus implementieren
```

---

## Architekturregeln

architecture:
layer_separation: required
cyclic_dependencies: forbidden
dependency_injection: required

layers:
controller: request_handling
service: business_logic
repository: data_access
dto: data_transport
mapper: transformation

---

## Testbarkeit

testing:
deterministic_tests: required
independent_tests: required
arrange_act_assert: required

forbidden_test_dependencies:
- ui
- external_framework_logic
- direct_database_logic

---

## Verbotene Muster

forbidden_patterns:
- god_classes
- spaghetti_code
- copy_paste_code
- magic_numbers
- magic_strings
- global_state
- mutable_static_variables
- nested_ternary_operators
- empty_catch_blocks
- dead_code
- unused_imports

---

## Logging

logging:
structured_logging: required
sensitive_data_logging: forbidden

forbidden_logged_data:
- passwords
- tokens
- credit_card_data
- private_sensitive_information

---

## KI-spezifische Regeln

ai_rules:
explicit_structure: required
implicit_framework_magic: forbidden
naming_consistency: required
formatting_consistency: required
deterministic_structure: required

goal:
- machine_readability
- automated_analysis
- predictable_refactoring
- low_ambiguity

---

## Review-Checkliste

review_checklist:
- class_max_100_lines
- method_max_60_lines
- nesting_max_3
- no_magic_numbers
- no_unused_imports
- semantic_names
- tests_exist
- exceptions_handled
- no_empty_catch
- architecture_valid
- structured_logging
- no_sensitive_logging
- no_dead_entry_points

---

## Prioritäten

priority_order:
1: correctness
2: security
3: readability
4: maintainability
5: performance
6: brevity

rule:
performance_optimizations_require_measurable_problem: true

---

## Abschlussregel

final_rule:
if_code_requires_extra_context:
action: simplify_structure
if_entry_point_unused:
action: remove_completely