# Enhance Lombok @ToString annotation


## How to use

---
### Define a Desensitize Tool
```java
package com.example;

public class DesensitizeUtil {

    /**
     * replace as * from 5 ~ 8
     */
    public static String desensitize(String param) {
        return param.substring(0, 4) + "****" + param.substring(8);
    }

}
```

---
### Your Entity
```java
package com.example;

import lombok.Data;
import lombok.ToString;

@Data
public class User {

    // class 2 method name
    @ToString.Handler("com.example.DesensitizeUtil.desensitize")
    private String password;

    // class and method name
    @ToString.Handler(handlerClass = DesensitizeUtil.class, handlerMethod = "desensitize")
    private String bankcard;

}
```

### Generated toString() in class file.
```java
public String toString() {
    return "User(password=" + DesensitizeUtil.desensitize(this.getPassword()) + ", bankcard=" + DesensitizeUtil.desensitize(this.getBankcard()) + ")";
}
```

---

## How to import

Run `ant maven` and upload to your private maven repo.

---



## you can see the code I change in history.

---

## if you have any idea please leave a comment.

---