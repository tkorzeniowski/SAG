## SAG
Systemy agentowe 18L.

### Temat
System sterowania Smart Grid.

Projekt polegał na wykorzystaniu systemu agentowego do zarządzania inteligentną siecią elektroenergetyczną, w której komunikacja pomiędzy wszystkimi uczestnikami rynku energii, pozwalałaby na obniżenie kosztów, zwiększenie efektywności oraz zintegrowanie rozproszonych źródeł energii.

### Założenia
* Klienci mogą być jednocześnie zarówno producentami jak i konsumentami medium, jednak w pierwszej kolejności wykorzystują medium, które wyprodukują
* Klient jest w stanie indywidualnie oszacować własne zapotrzebowanie lub produkcję, które zgłasza (nadzorcy obszaru) na początku okresu poprzedniego
* Koszt przesyłu medium zależny jest od odległości między użytkownikami systemu (obliczana na podstawie położenia geograficznego klientów)
* W jednym obszarze występuje przynajmniej jeden klient, pełniący rolę producenta i dostawcy medium.
* Każdy obszar ma jednego nadzorcę oraz jeden kontroler sieci, które ze sobą współpracują, a także komunikują się z klientami w celu wyznaczenia optymalnego planu dostaw. Ponadto nadzorca może prowadzić negocjacje z nadzorcami sąsiednich obszarów, jeśli produkcja wewnątrz monitorowanego terytorium nie pokrywa zapotrzebowania.
* Istnieje także nadzorca wszystkich obszarów, którego zadaniem jest przechowywanie aktualnego stanu wszystkich nadzorców, zapewniając możliwość naprawy systemu w sytuacji awarii czy wystąpienia nieoczekiwanego błędu.

Implementacja została wykonana w języku Java (Maven) z wykorzystaniem biblioteki [Akka](https://akka.io/).
