package openrewrite;

import java.net.MalformedURLException;
import java.net.URL;

public class ToBeUpdated {

    public static void main(String[] args) throws MalformedURLException {

        // This code has to be updated by the URLConstructorToURICreate recipe
        URL url1 = new URL("http://www.google.com") ;
        URL url2 = new URL("http://www.google.com") ;
        URL url3 = new URL("http://www.google.com") ;

    }
}
