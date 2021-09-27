# registry-regulations-publication-ci-stages

Дана groovy бібліотека є розширенням стандартної EDP
бібліотеки [edp-library-stages](https://github.com/epmd-edp/edp-library-stages/tree/release-2.6) і містить опис
послідовності дій для автоматизованої публікації змін компонентів
(бізнес процеси, бізнес правила та форми) до регламенту реєстру.

### Огляд процеcу публікації змін

Процес публікації змін складаєтся з наступних етапів:

* Адміністратор регламенту в локальній копії git репозиторію реєстру створює нові та/або змінює існуючі компоненти
  регламенту реєстру.
* Використовуючи git клієнт, адміністратор завантажує зміни для процессу розгляду в репозиторій реєстру, що зберігаєтся
  в Gerrit.
* Якщо зміни успішно проходять процес розгляду, адміністратор проводить операцію merge змін у master гілку репозиторію.
* Jenkins пайплайн публікації змін автоматично запускаєтся після операції merge до master гілки.
* Для виконання дій з публікації змін Jenkins створює новий агент, якщо немає все існуючого вільного агента.
* Jenkins агент завантажує останню версію репозиторію регламента реєстру і проводить аналіз змін у бізнес процесах,
  бізнес правилах і формах.
* Зміни у бізнес процесах/правилах і формах, якщо вони є, публікуються, відповідно, до camunda та formio сервісів
  розгорнутих у тентанті реєстру.

Наведена діаграма іллюструє описаний процес:

![Alt text](resources/diagrams/RegistryPublicationProcessOverview.png?raw=true)

### Jenkins пайплайн публікації змін

#### Огляд

Пайплайн публікації змін складаєтся з наступних етапів:

![Alt text](resources/diagrams/RegistryPublicationPipeline.png?raw=true)

* Init - технічний етап. Виконуєтся логіка описана в файлі Build.groovy в репозиторії
  [registry-regulations-publication-pipelines](https://gerrit-mdtu-ddm-edp-cicd.apps.cicd.mdtu-ddm.projects.epam.com/admin/repos/registry-regulations-publication-pipelines)
  (розширення стандартної EDP
  бібліотеки [edp-library-pipelines](https://github.com/epmd-edp/edp-library-pipelines/tree/release-2.6))
* checkout - етап завантаження репозиторію регламенту реєстру. Використувує оригінальний EDP checkout етап (див.
  src/com/epam/edp/stages/impl/ci/impl/checkout/Checkout.groovy)
* get-changes - етап, що визначає які компоненти регламента були змінені, перевіряючи чи були створені або редаговані
  файли у папках репозіторія: bpmn (визначення бізнес процесів), dmn (визначення бізнес правил) і forms (форми).
* upload-business-process-changes - на даному етапі до camunda сервісу публікуються нові та/або редаговані існуючі
  бізнес процеси і бізнес правила.
* upload-form-changes - на даному етапі до formio сервісу публікуються нові та/або редаговані існуючі форми.

*Етапи get-changes, upload-business-process-changes, upload-form-changes є спеціально розробленими. Імплементація може
бути знайдена в папці src/com/epam/edp/customStages/impl/bpm у відповідних groovy файлах.*

#### Низькорівневий дизайн пайплайну публікації змін

![Alt text](resources/diagrams/RegistryPublicationPipelineLowLevelDesign.png?raw=true)
