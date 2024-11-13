import React, {useEffect, useState} from "react"
import dayjs from "dayjs"
import LocalizedFormat from "dayjs/plugin/localizedFormat"
import {UIConfig} from "./types"
import {UI} from "./UI"
import {Loading} from "./Loading"

require("dayjs/locale/fi")

dayjs.locale("fi")
dayjs.extend(LocalizedFormat)

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
    <UI lang={lang!} uiConfig={uiConfig!}/>
  ) : (
    <Loading/>
  )
}
