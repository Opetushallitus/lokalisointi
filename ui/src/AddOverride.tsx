import React, {ChangeEvent, useEffect, useState} from "react"
import {
  Autocomplete,
  FormControl,
  FormLabel,
  IconButton,
  TableCell,
  TableRow,
  TextField
} from "@mui/material"
import {Cancel, Save} from "@mui/icons-material"
import {OphSelect} from "@opetushallitus/oph-design-system"
import {useTranslate} from "@tolgee/react"
import {useSnackbar} from "notistack"

interface Props {
  close: () => void,
}

export const AddOverride: React.FC<Props> = ({close}) => {
  const [availableNamespaces, setAvailableNamespaces] = useState<string[]>([])
  const [namespace, setNamespace] = useState<string | undefined>(undefined)
  const [key, setKey] = useState<string>("")
  const [locale, setLocale] = useState<string>("")
  const [value, setValue] = useState<string>("")
  const {t} = useTranslate()
  const {enqueueSnackbar} = useSnackbar()
  useEffect(() => {
    if (enqueueSnackbar && t) {
      fetch("/lokalisointi/api/v1/override/available-namespaces", {
        method: "GET"
      }).then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          enqueueSnackbar(t("namespaces-could-not-be-loaded", "Nimiavaruuksia ei saatu ladattua. Yritä myöhemmin uudelleen."), {variant: "error"})
          return
        }
        setAvailableNamespaces(body)
      })
    }
  }, [enqueueSnackbar, t])
  const save = () => {
    fetch("/lokalisointi/api/v1/override", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify({namespace: namespace, locale: locale, key: key, value: value})
    })
      .then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          enqueueSnackbar(t("override-save-failed", {
            body: JSON.stringify(body)
          }), {variant: "error"})
          return
        }
        setNamespace(undefined)
        setKey("")
        setLocale("")
        setValue("")
        close()
      })
  }
  return (
    <TableRow>
      <TableCell></TableCell>
      <TableCell>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="namespace">{t("column-namespace", "nimiavaruus")}</FormLabel>
          <Autocomplete
            id="namespace"
            freeSolo
            disableClearable
            value={namespace}
            onChange={(_, value) => {
              setNamespace(value)
            }}
            options={availableNamespaces.map((option) => option)}
            renderInput={(params) => (
              <TextField
                {...params}
                variant="outlined"
                size="small"
                onChange={(e) => {
                  setNamespace(e.target.value)
                }}
                slotProps={{
                  input: {
                    ...params.InputProps,
                    type: 'search',
                  },
                }}
              />
            )}
          />
        </FormControl>
      </TableCell>
      <TableCell>
        <FormControl fullWidth>
          <FormLabel htmlFor="key">{t("column-key", "avain")}</FormLabel>
          <TextField id="key" variant="outlined" size="small" value={key}
                     onChange={(e) => setKey(e.target.value)}/>
        </FormControl>
      </TableCell>
      <TableCell>
        <FormControl fullWidth>
          <FormLabel htmlFor="locale">{t("column-locale", "kieli")}</FormLabel>
          <OphSelect id="locale" value={locale} size="small"
                     onChange={(e) => setLocale(e.target.value)}
                     options={[
                       {label: "fi", value: "fi"},
                       {label: "sv", value: "sv"},
                       {label: "en", value: "en"}
                     ]}/>
        </FormControl>
      </TableCell>
      <TableCell>
        <FormControl fullWidth>
          <FormLabel htmlFor="value">{t("column-value", "arvo")}</FormLabel>
          <TextField id="value" value={value} multiline size="small" variant="outlined"
                     onChange={(e: ChangeEvent<HTMLTextAreaElement>) =>
                       setValue(e.target.value)}/>
        </FormControl>
      </TableCell>
      <TableCell colSpan={5} sx={{verticalAlign: "bottom"}}>
        <IconButton onClick={save} disabled={!key || !locale || !value}
                    color="primary" title={t("save", "tallenna")}><Save/></IconButton>
        <IconButton onClick={close} title={t("cancel", "peruuta")}><Cancel/></IconButton>
      </TableCell>
    </TableRow>
  )
}
