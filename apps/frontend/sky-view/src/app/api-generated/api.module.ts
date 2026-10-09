import { NgModule, ModuleWithProviders, SkipSelf, Optional } from '@angular/core';
import { SkyApiConfiguration } from './configuration';
import { HttpClient } from '@angular/common/http';


@NgModule({
  imports:      [],
  declarations: [],
  exports:      [],
  providers: []
})
export class SkyApiApiModule {
    public static forRoot(configurationFactory: () => SkyApiConfiguration): ModuleWithProviders<SkyApiApiModule> {
        return {
            ngModule: SkyApiApiModule,
            providers: [ { provide: SkyApiConfiguration, useFactory: configurationFactory } ]
        };
    }

    constructor( @Optional() @SkipSelf() parentModule: SkyApiApiModule,
                 @Optional() http: HttpClient) {
        if (parentModule) {
            throw new Error('SkyApiApiModule is already loaded. Import in your base AppModule only.');
        }
        if (!http) {
            throw new Error('You need to import the HttpClientModule in your AppModule! \n' +
            'See also https://github.com/angular/angular/issues/20575');
        }
    }
}
