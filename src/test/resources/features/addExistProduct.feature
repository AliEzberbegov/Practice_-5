# language: ru

@all
@addExistProduct
Функция: Добавление существующего продукта в список товаров

Предыстория:
  * стенд QualIT запущен и подключен к БД, страница "http://localhost:8080" открыта

Структура сценария: Добавление существующего товара
  * товар <productName> присутствует в БД
  * пользователь добавляет существующий товар с наименованием <productName>, типом <productType> и статусом чекбокса <exoticStatus>
  * продукт <productName> успешно добавлен и отображается в списке товаров
  * продукт <productName> найден в БД
  * выводятся все записи в БД с наименованием продукта <productName>
  * пользователь получает сообщение об ошибке дублирования продукта <productName>

  Примеры:
    | productName | productType | exoticStatus      |
    | "Помидор"   | "Овощ"      | "не экзотический" |
    | "Капуста"   | "Овощ"      | "не экзотический" |