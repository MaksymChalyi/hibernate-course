package ua.minibank;

/** Резерв: DTO-проєкція. Hibernate одразу створює record із колонок, без entity і Persistence Context. */
public record AccountOwnerView(String accountNumber, String firstName, String lastName) {

    public String line() {
        return accountNumber + " — " + firstName + " " + lastName;
    }
}
