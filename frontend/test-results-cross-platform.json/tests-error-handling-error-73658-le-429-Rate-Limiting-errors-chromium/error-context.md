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
        - textbox "Email" [ref=e17]: e2e_test_user
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
    - alert [ref=e30]:
      - img [ref=e32]
      - generic [ref=e34]: Email is required
    - button "Sign In" [active] [ref=e35] [cursor=pointer]:
      - img [ref=e37] [cursor=pointer]
      - text: Sign In
    - button "Forgot password?" [ref=e40] [cursor=pointer]
    - paragraph [ref=e42]:
      - text: Don't have an account?
      - button "Sign up" [ref=e43] [cursor=pointer]
  - generic [ref=e44]:
    - img [ref=e46]
    - button "Open Tanstack query devtools" [ref=e94] [cursor=pointer]:
      - img [ref=e95] [cursor=pointer]
```