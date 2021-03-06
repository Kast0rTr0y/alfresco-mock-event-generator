/*
 * Copyright 2018 Alfresco Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.mockeventgenerator.util;

/**
 * @author Jamal Kaabi-Mofrad
 */
public class UserInfo
{
    private String firstName;
    private String lastName;
    private int age;
    private String userName;

    public UserInfo()
    {
    }

    public UserInfo(String firstName, String lastName, int age, String userName)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.userName = userName;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public int getAge()
    {
        return age;
    }

    public void setAge(int age)
    {
        this.age = age;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(100);
        sb.append("UserInfo [firstName=").append(firstName)
                    .append(", lastName=").append(lastName)
                    .append(", age=").append(age)
                    .append(", userName=").append(userName)
                    .append(']');
        return sb.toString();
    }
}
