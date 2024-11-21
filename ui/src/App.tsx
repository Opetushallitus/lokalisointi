import React, {useEffect, useState} from "react"
import dayjs from "dayjs"
import LocalizedFormat from "dayjs/plugin/localizedFormat"
import {OphThemeProvider} from "@opetushallitus/oph-design-system/theme"
import {DevBackend, Tolgee} from "@tolgee/web"
import {BackendFetch, FormatSimple, TolgeeProvider} from "@tolgee/react"
import {UIConfig} from "./types"
import {UI} from "./UI"
import {Loading} from "./Loading"
import {OphLanguage} from "@opetushallitus/oph-design-system"

import "dayjs/locale/fi"

dayjs.locale("fi")
dayjs.extend(LocalizedFormat)

interface TranslatedProps {
  lang: OphLanguage
  uiConfig: UIConfig
  children: React.ReactNode
}

const WithProviders: React.FC<TranslatedProps> = ({lang, uiConfig, children}) => {
  const tolgeeChain = Tolgee()
    .use(FormatSimple())
    .use(BackendFetch({
      // this can point to OPH CloudFront once configured
      prefix: "https://cdn.tolg.ee/757120c6367d7d321f780779868b1802"
    }))
  if (uiConfig.currentEnvironment === "pallero") {
    tolgeeChain.use(DevBackend())
  }
  const tolgee = tolgeeChain
    .init({
      language: lang,
      availableLanguages: ["fi", "sv", "en"],
      defaultNs: "lokalisointi",
      ns: ["lokalisointi"],
      projectId: 11100
    })
  return (
    <TolgeeProvider
      tolgee={tolgee}
      fallback={<Loading/>}
    >
      <OphThemeProvider variant="oph" lang={lang}>
        {children}
      </OphThemeProvider>
    </TolgeeProvider>
  )
}

export const App: React.FC = () => {
  const [uiConfig, setUiConfig] = useState<undefined | UIConfig>(undefined)
  const [lang, setLang] = useState<undefined | "fi" | "sv" | "en">(undefined)

  useEffect(() => {
    fetch("/lokalisointi/api/v1/ui-config").then(async response => {
      if (response.ok) {
        setUiConfig(await response.json())
      }
    })
    fetch(`/kayttooikeus-service/cas/me`).then(async response => {
      if (response.ok) {
        const me = await response.json()
        if (me.lang) {
          setLang(me.lang)
        }
      }
    })
  }, [])

  return !!lang && !!uiConfig ? (
    <WithProviders lang={lang!} uiConfig={uiConfig!}>
      <UI uiConfig={uiConfig!}/>
    </WithProviders>
  ) : (
    <Loading/>
  )
}
