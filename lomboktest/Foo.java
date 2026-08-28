import lombok.Builder;
import lombok.Getter;
@Builder
public class Foo {
    @Getter private final String name;
    public static void main(String[] a){ System.out.println(Foo.builder().name("x").build().getName()); }
}
