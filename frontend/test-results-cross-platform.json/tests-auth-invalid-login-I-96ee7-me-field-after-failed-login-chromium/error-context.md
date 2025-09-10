# Page snapshot

```yaml
- generic [ref=e2]:
  - generic [ref=e8]:
    - heading "Sign In" [level=1] [ref=e9]
    - paragraph [ref=e10]: Welcome back to FocusHive
    - generic [ref=e11]:
      - generic [ref=e12]: Email
      - generic [ref=e13]:
        - img [ref=e15]
        - textbox "Email" [ref=e17]: testuser@example.com
        - group:
          - generic: Email
    - generic [ref=e18]:
      - generic [ref=e19]: Password
      - generic [ref=e20]:
        - img [ref=e22]
        - textbox "Password" [ref=e24]: wrongpassword
        - button "Toggle password visibility" [ref=e26] [cursor=pointer]:
          - img [ref=e27] [cursor=pointer]
        - group:
          - generic: Password
    - button "Sign In" [ref=e29] [cursor=pointer]:
      - img [ref=e31] [cursor=pointer]
      - text: Sign In
    - button "Forgot password?" [ref=e34] [cursor=pointer]
    - paragraph [ref=e36]:
      - text: Don't have an account?
      - button "Sign up" [ref=e37] [cursor=pointer]
  - generic [ref=e38]:
    - img [ref=e40]
    - button "Open Tanstack query devtools" [ref=e88] [cursor=pointer]:
      - img [ref=e89] [cursor=pointer]
```